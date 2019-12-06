package com.xf.glmall.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xf.glmall.Util.RedisUtil;
import com.xf.glmall.dao.UmsMemberReceiveAddressMapper;
import com.xf.glmall.dao.UserMapper;
import com.xf.glmall.entity.UmsMember;
import com.xf.glmall.entity.UmsMemberReceiveAddress;
import com.xf.glmall.service.UserService;
import com.xf.glmall.util.MD5util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    String umsMemberKey = "umsMenber";
    String logKey = "umsMemberInfo";
    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    MD5util md5Util;

    @Override
    public List<UmsMember> getAllUser() {


        List<UmsMember> umsMemberList = new ArrayList<>();
        //连接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String umsJson = jedis.get(umsMemberKey);
        if (StringUtils.isNotBlank(umsJson)) {
            umsMemberList = JSON.parseObject(umsJson, new TypeReference<ArrayList<UmsMember>>() {
            });
        } else {
            //如果缓存中没有，
            //设置redis分布式锁

            String token = UUID.randomUUID().toString();
            /**
             * nx 设置分布式锁，
             * px 设置过期时间
             * time 时间(1000毫秒=1秒)
             */
            String key = jedis.set(umsMemberKey, token, "nx", "px", 1000);

            //判断锁是否设置成功
            if (StringUtils.isNotBlank(key) && "ok".equals(key)) {
                //设置成功的话，则有权在10秒内访问数据库
                // 则查询mysql数据库
                umsMemberList = getAllUserFormDb();

                if (umsMemberList != null) {
                    //如果数据库中有数据则将数据放入redis中
                    jedis.set(umsMemberKey, JSON.toJSONString(umsMemberList));
                } else {
                    //如果数据库中没有该数据则将redis中这个数据设为null ，防止缓存穿透
                    //将空值设置一个过期时间
                    jedis.setex(umsMemberKey, 60 * 3, JSON.toJSONString(""));
                }

                //或者当前锁value值
                String lockToken = jedis.get(umsMemberKey);
                //为了保证用户删除的是自己设置的锁，防止删除别的用户设置的锁
                if (StringUtils.isNotBlank(lockToken) && token.equals(lockToken)) {
                    //在成功访问mysql之后，将redis锁释放掉
                    jedis.del(umsMemberKey);
                }

            } else {
                //如果设置失败,则让这个线程睡几秒后再继续执行
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //使用return则不会启动新的线程，如果直接调用则会启动新的线程
                return getAllUser();
            }

        }

        return umsMemberList;
    }


    public List<UmsMember> getAllUserFormDb() {
        List<UmsMember> umsMemberList = userMapper.selectAll();//userMapper.selectAllUser();

        return umsMemberList;
    }


    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        // 封装的参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

//        Example example = new Example(UmsMemberReceiveAddress.class);
//        example.createCriteria().andEqualTo("memberId",memberId);
//        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(example);

        return umsMemberReceiveAddresses;
    }


    @Override
    public UmsMember getlogin(UmsMember umsMember) {

        umsMember.setPassword(md5Util.MD5PasswordEncryption(umsMember.getUsername(), logKey, umsMember.getPassword()));

        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            if (jedis != null) {

                String umsMemberStr = jedis.get("user:" + umsMember.getUsername() + umsMember.getPassword() + ":info");

                if (StringUtils.isNotBlank(umsMemberStr)) {

                    //密码正确
                    UmsMember umsMemberCache = JSON.parseObject(umsMemberStr, UmsMember.class);

                    return umsMemberCache;
                }
            }
            //查询数据库，并将
            UmsMember umsMemberDb = loginFromDb(umsMember);

            if(umsMemberDb!=null){

                jedis.setex("user:" + umsMemberDb.getUsername() + umsMemberDb.getPassword() + ":info",60*60*24,JSON.toJSONString(umsMemberDb));
            }
            return umsMemberDb;

        } finally {
            jedis.close();
        }
    }

    /**
     * 将生成的登录token放入redis
     * @param token
     * @param memberId
     */
    @Override
    public void addUserToken(String token, String memberId) {

        Jedis jedis =redisUtil.getJedis();

        try {
            jedis.setex("user:"+memberId+":token",60*60*2,token);
        }finally {
            jedis.close();
        }

    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
         userMapper.insertSelective(umsMember);
         return umsMember;
    }

    @Override
    public UmsMember checkOauthUser(UmsMember checkUms) {
        UmsMember umsMember = userMapper.selectOne(checkUms);
        return umsMember;
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress address =new UmsMemberReceiveAddress();
        address.setId(receiveAddressId);
        UmsMemberReceiveAddress receiveAddress = umsMemberReceiveAddressMapper.selectOne(address);
        return receiveAddress;
    }

    private UmsMember loginFromDb(UmsMember umsMember) {
        List<UmsMember> umsMemberDb=userMapper.select(umsMember);
        if(umsMemberDb.size()>0){
            return umsMemberDb.get(0);
        }
        return null;
    }
}
