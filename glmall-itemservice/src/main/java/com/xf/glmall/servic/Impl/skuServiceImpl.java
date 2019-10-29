package com.xf.glmall.servic.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xf.glmall.Util.RedisUtil;
import com.xf.glmall.dao.pmsSkuImageMapper;
import com.xf.glmall.dao.pmsSkuInfoMapper;
import com.xf.glmall.entity.PmsSkuImage;
import com.xf.glmall.entity.PmsSkuInfo;
import com.xf.glmall.service.skuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.UUID;

@Service
public class skuServiceImpl implements skuService {

    String skuInfoKey="skuInfo";
    String onLockKey="skuInfOnLock";

    @Autowired
    pmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    pmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public PmsSkuInfo getSkuById(String skuId) {


        PmsSkuInfo skuInfo=new PmsSkuInfo();

        Jedis jedis=redisUtil.getJedis();

        String skuInfoJson = jedis.get(skuInfoKey+skuId);

        //判断redis是否存在这个数据，如果存在，就在redis中将这个数据取出
        if(StringUtils.isNotBlank(skuInfoJson)){

            skuInfo = JSON.parseObject(skuInfoJson,new TypeReference<PmsSkuInfo>(){});

        }else{

            //设置随机值，放入分布式锁中
            String token= UUID.randomUUID().toString();

            /**
             * nx 设置分布式锁，
             * px 设置过期时间
             * time 时间(1000毫秒=1秒)
             */
            String key = jedis.set(onLockKey+skuId,token,"nx","px",1000);

            //判断锁是否设置成功
            if(StringUtils.isNotBlank(key)&&"OK".equals(key)){
                skuInfo = getSynSkuById(skuId);

                if(skuInfo!=null){
                    //数据库中有数据则将数据放入到redis中
                    jedis.set(skuInfoKey+skuId,JSON.toJSONString(skuInfo));

                }else{
                    //如果数据库中没有这个数据，则在redis中添加这个数据的空值，并将这个值设置过期时间
                    jedis.setex(skuInfoKey+skuId,60*3,JSON.toJSONString(""));
                }

                //获取当前分布式锁的token值
//                并判断是不是同一个用户操作
                String lockToken=jedis.get(onLockKey+skuId);

                if(StringUtils.isNotBlank(lockToken)&&token.equals(lockToken)){
                    jedis.del(onLockKey+skuId);
                }

            }else{
                //如果设置锁失败，则让这个线程睡几秒后再执行

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //return回去不会启动新的线程
                return getSkuById(skuId);
            }
        }
        return skuInfo;
    }


    public PmsSkuInfo getSynSkuById(String skuId){
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //查询图片列表
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> PmsSkuImageList = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setPmsSkuImageList(PmsSkuImageList);
        return skuInfo;
    }


    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }


}
