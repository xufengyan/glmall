package com.xf.glmall.servic.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.xf.glmall.Util.RedisUtil;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    JestClient jestClient;

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

    @Override
    public List<PmsSkuInfo> getAllSku() {

        List<PmsSkuInfo> pmsSkuInfos= pmsSkuInfoMapper.selectSkuAll();

        return pmsSkuInfos;
    }



    @Override
    public List<PmsSearchSkuInfo> getSearchSkuinfoByPmaram(PmsSearchParam pmsSearchParam) {

       String delStr=getSearchStr(pmsSearchParam);

        System.out.println("查询语句"+delStr);

        List<PmsSearchSkuInfo> searchSkuInfos=new ArrayList<>();
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
            PmsSearchSkuInfo pmsSearchSkuInfo=hit.source;
            Map<String, List<String>> highlight = hit.highlight;

            if(highlight!=null){
                String skuName = highlight.get("skuName").get(0);
                pmsSearchSkuInfo.setSkuName(skuName);
            }

            searchSkuInfos.add(pmsSearchSkuInfo);
        }

        return searchSkuInfos;
    }




    private String getSearchStr(PmsSearchParam pmsSearchParam) {
        String [] pmsSkuAttrValueList = pmsSearchParam.getValueId();

        //       java中的 elasticsearch查询语句

        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();

        //查询条件语句------------------------------------------------------------
        //bool
        BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();

        //filer

        //根据三级分类id查询
        if(StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())){
            TermQueryBuilder term=new TermQueryBuilder("catalog3Id",pmsSearchParam.getCatalog3Id());//过滤条件 (目标,目标值)
            boolQueryBuilder.filter(term);
        }


        //根据商品属性id查询
        if(pmsSkuAttrValueList!=null){

            for (String pmsSkuAttrValue:pmsSkuAttrValueList){
                TermQueryBuilder term=new TermQueryBuilder("pmsSkuAttrValueList.valueId",pmsSkuAttrValue);//过滤条件 (目标,目标值)
                boolQueryBuilder.filter(term);
            }

        }

        //must
        //模糊查询
        if(StringUtils.isNotBlank(pmsSearchParam.getKeyword())){
            MatchQueryBuilder match=new MatchQueryBuilder("skuName",pmsSearchParam.getKeyword());
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


        HighlightBuilder highlightBuilder=new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");

                //highlight 设置高亮
        searchSourceBuilder.highlighter(highlightBuilder);

        return searchSourceBuilder.toString();
    }


}
