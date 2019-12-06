package com.xf.glmall.servic.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.xf.glmall.Util.RedisUtil;
import com.xf.glmall.dao.omsCartItemMapper;
import com.xf.glmall.dao.pmsSkuImageMapper;
import com.xf.glmall.dao.pmsSkuInfoMapper;
import com.xf.glmall.entity.*;
import com.xf.glmall.service.skuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class skuServiceImpl implements skuService {

    //redis 商品信息key
    String skuInfoKey = "skuInfo";
    //redis 分布式锁key
    String onLockKey = "skuInfOnLock";
    //redis 购物车信息key
    String cartItemKey = "cartItemKey";

    String cartLockKey = "cartLockKey";

    @Autowired
    pmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    pmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    omsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    JestClient jestClient;

    @Override
    public PmsSkuInfo getSkuById(String skuId) {


        PmsSkuInfo skuInfo = new PmsSkuInfo();

        Jedis jedis = redisUtil.getJedis();

        String skuInfoJson = jedis.get(skuInfoKey + skuId);

        //判断redis是否存在这个数据，如果存在，就在redis中将这个数据取出
        if (StringUtils.isNotBlank(skuInfoJson)) {

            skuInfo = JSON.parseObject(skuInfoJson, new TypeReference<PmsSkuInfo>() {
            });

        } else {

            //设置随机值，放入分布式锁中
            String token = UUID.randomUUID().toString();
            /**
             * nx 设置分布式锁，
             * px 设置过期时间
             * time 时间(1000毫秒=1秒)
             */
            String key = jedis.set(onLockKey + skuId, token, "nx", "px", 1000);

            //判断锁是否设置成功
            if (StringUtils.isNotBlank(key) && "OK".equals(key)) {
                skuInfo = getSynSkuById(skuId);

                if (skuInfo != null) {
                    //数据库中有数据则将数据放入到redis中
                    jedis.set(skuInfoKey + skuId, JSON.toJSONString(skuInfo));

                } else {
                    //如果数据库中没有这个数据，则在redis中添加这个数据的空值，并将这个值设置过期时间
                    jedis.setex(skuInfoKey + skuId, 60 * 3, JSON.toJSONString(""));
                }

                //获取当前分布式锁的token值
//                并判断是不是同一个用户操作
                String lockToken = jedis.get(onLockKey + skuId);
                String orderScript = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                        "then return redis.call('del', KEYS[1]) else return " +
                        "0 end";
                //使用lua脚本判断是不是当前用户的锁，是的话删除，不是的话则返回0
                Long eval = (Long) jedis.eval(orderScript, Collections.singletonList(onLockKey + skuId),
                        Collections.singletonList(token));


            } else {
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


    public PmsSkuInfo getSynSkuById(String skuId) {
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

    @Override
    public List<PmsSkuInfo> getAllSku() {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuAll();

        return pmsSkuInfos;
    }


    @Override
    public List<PmsSearchSkuInfo> getSearchSkuinfoByPmaram(PmsSearchParam pmsSearchParam) {

        String delStr = getSearchStr(pmsSearchParam);

        System.out.println("查询语句" + delStr);

        List<PmsSearchSkuInfo> searchSkuInfos = new ArrayList<>();
        //执行elasticsearch查询
        /**
         * Builder:查询过滤的条件语句
         * addIndex :库名
         * addtype：表名
         */
        Search search = new Search.Builder(delStr).addIndex("glmall").addType("pmsSkuInfo").build();

        SearchResult execute = null;
        try {
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //接收elasticsearch中查询出来的值
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {

            //拿得查询的对象
            PmsSearchSkuInfo pmsSearchSkuInfo = hit.source;
            Map<String, List<String>> highlight = hit.highlight;

            if (highlight != null) {
                String skuName = highlight.get("skuName").get(0);
                pmsSearchSkuInfo.setSkuName(skuName);
            }

            searchSkuInfos.add(pmsSearchSkuInfo);
        }

        return searchSkuInfos;
    }

    private String getSearchStr(PmsSearchParam pmsSearchParam) {
        String[] pmsSkuAttrValueList = pmsSearchParam.getValueId();

        //       java中的 elasticsearch查询语句

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //查询条件语句------------------------------------------------------------
        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //filer

        //根据三级分类id查询
        if (StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())) {
            TermQueryBuilder term = new TermQueryBuilder("catalog3Id", pmsSearchParam.getCatalog3Id());//过滤条件 (目标,目标值)
            boolQueryBuilder.filter(term);
        }


        //根据商品属性id查询
        if (pmsSkuAttrValueList != null) {

            for (String pmsSkuAttrValue : pmsSkuAttrValueList) {
                TermQueryBuilder term = new TermQueryBuilder("pmsSkuAttrValueList.valueId", pmsSkuAttrValue);//过滤条件 (目标,目标值)
                boolQueryBuilder.filter(term);
            }

        }

        //must
        //模糊查询
        if (StringUtils.isNotBlank(pmsSearchParam.getKeyword())) {
            MatchQueryBuilder match = new MatchQueryBuilder("skuName", pmsSearchParam.getKeyword());
            boolQueryBuilder.must(match);
        }


        //bool中设置的将查询条件语句放入query中执行--------------------------------------------
        //query
        searchSourceBuilder.query(boolQueryBuilder);

        //elasticSearch分页查询------------------------------------------------
        //from 从多少开始条查
        searchSourceBuilder.from(0);
        //size 查询条数
        searchSourceBuilder.size(20);


        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");

        //highlight 设置高亮
        searchSourceBuilder.highlighter(highlightBuilder);

        return searchSourceBuilder.toString();
    }


    //    购物车业务---------------------------------------------------------------------


    @Override
    public OmsCartItem ifCartExistByUser(String menberId, String skuId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(menberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem omsCartItemDb = omsCartItemMapper.selectOne(omsCartItem);

        return omsCartItemDb;
    }

    @Override
    public int updateCart(OmsCartItem omsCartItemFromDb) {

        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("id", omsCartItemFromDb.getId());
        int message = omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb, e);

        return message;
    }

    @Override
    public int addCart(OmsCartItem omsCartItem) {

        int messasge = 0;
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())) {
            omsCartItem.setId("" + UUID.randomUUID());
            messasge = omsCartItemMapper.insertSelective(omsCartItem);
        }
        return messasge;
    }

    @Override
    public List<OmsCartItem> flushCartCahe(String menberId) {
        Jedis jedis = redisUtil.getJedis();
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try {
//        同步用户购物车数据
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(menberId);
            omsCartItems = omsCartItemMapper.select(omsCartItem);

            Map<String, String> map = new HashMap<>();
            for (OmsCartItem cartItem : omsCartItems) {
                cartItem.setTotalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
            }

//            先删除，再添加
            jedis.del(cartItemKey + ":" + menberId);
            //        同步到redis缓存中
            jedis.hmset(cartItemKey + ":" + menberId, map);
        } catch (Exception e) {

        } finally {
            jedis.close();

        }
        return omsCartItems;
    }

    @Override
    public List<OmsCartItem> cartList(String userId) {

        Jedis jedis = null;

        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try {
            jedis = redisUtil.getJedis();
            //查询redis缓存中的购物车数据
            List<String> hvals = jedis.hvals(cartItemKey +":"+ userId);


            if (hvals != null) {
                for (String hval : hvals) {
                    OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                    omsCartItems.add(omsCartItem);
                }
            } else {
                //设置分布式锁的值
                String token=UUID.randomUUID().toString();

                //添加分布式锁
                String key = jedis.set(cartLockKey + ":" + userId, token, "nx", "px", 1000);

                //判断分布式锁是否设置成功

                if(StringUtils.isNotBlank(key)&&"OK".equals(key)){
                    //如果设置成功,则查询数据库
                    OmsCartItem omsCartItem = new OmsCartItem();
                    omsCartItem.setMemberId(userId);
                    omsCartItems = omsCartItemMapper.select(omsCartItem);
                    Map<String,String> map=new HashMap<>();

                    if(omsCartItems!=null){

                        for (OmsCartItem cartItem : omsCartItems) {
                            cartItem.setTotalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                            map.put(cartItem.getProductSkuId(),JSON.toJSONString(cartItem));
                        }
                        //将数据放入redis里面
                        jedis.hmset(cartItemKey+":"+userId,map);

                    }else {
                        jedis.hmset(cartItemKey+":"+userId,map);
                        jedis.expire(cartItemKey+":"+userId,60);
                    }

                    //使用完毕后删除分布式锁
                    String lockToken = jedis.get(cartLockKey + userId);

                    if (StringUtils.isNotBlank(lockToken) && token.equals(lockToken)) {
                        jedis.del(cartLockKey + userId);
                    }

                }else {
                    //分布式锁设置失败
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //return回去不会启动新的线程
                    return cartList(userId);
                }
            }

        } catch (Exception e) {
            //异常处理

            return null;
        } finally {

            jedis.close();

        }


        return omsCartItems;
    }

    @Override
    public int checkCart(OmsCartItem omsCartItem) {
        Example e=new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId());
        int message = omsCartItemMapper.updateByExampleSelective(omsCartItem, e);
        //缓存同步
        flushCartCahe(omsCartItem.getMemberId());
        return message;
    }

    @Override
    public Boolean checkPrice(String productId, BigDecimal productPrice) {

        Boolean bool = false;
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        pmsSkuInfo.setId(productId);

        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal price = skuInfo.getPrice();

        if(price.compareTo(productPrice)==0){
            bool = true;
        }
        return bool;
    }

    /**
     *
     * 删除购物车业务
     * @param prodoctId
     * @return
     */
    @Override
    public int delCartById(String prodoctId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setProductId(prodoctId);
        int msg = omsCartItemMapper.delete(omsCartItem);
        return msg;
    }


}
