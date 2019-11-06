package com.xf.glmall;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xf.glmall.entity.PmsSearchSkuInfo;
import com.xf.glmall.entity.PmsSkuInfo;
import com.xf.glmall.service.skuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GlmallItemserviceApplicationTests {


    @Reference
    skuService skuService;

    @Autowired
    JestClient jestClient;


    @Test
    public void contextLoads() throws IOException {

//       java中的 elasticsearch查询语句

        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();

        //查询条件语句------------------------------------------------------------
        //bool
        BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();

        //filer

        TermQueryBuilder term=new TermQueryBuilder("pmsSkuAttrValueList.valueId","43");//过滤条件 (目标,目标值)
        boolQueryBuilder.filter(term);

        //must
        MatchQueryBuilder match=new MatchQueryBuilder("skuName","测试");
        boolQueryBuilder.must(match);

        //bool中设置的将查询条件语句放入query中执行--------------------------------------------
        //query
        searchSourceBuilder.query(boolQueryBuilder);

        //elasticSearch分页查询------------------------------------------------
        //from 从多少开始条查
        searchSourceBuilder.from(0);
        //size 查询条数
        searchSourceBuilder.size(20);
        //highlight 设置高亮
        searchSourceBuilder.highlighter();

        System.out.println("查询语句"+searchSourceBuilder.toString());

        List<PmsSearchSkuInfo> searchSkuInfos=new ArrayList<>();
        //执行elasticsearch查询
        /**
         * Builder:查询过滤的条件语句
         * addIndex :库名
         * addtype：表名
         */
        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex("glmall").addType("pmsSkuInfo").build();

        SearchResult execute = jestClient.execute(search);

        //接收elasticsearch中查询出来的值
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {

            //拿得查询的对象
            PmsSearchSkuInfo pmsSearchSkuInfo=hit.source;
            searchSkuInfos.add(pmsSearchSkuInfo);
        }



    }


    /**
     * 将数据放入到elasticsearch中
     * @throws IOException
     */
    public void put() throws IOException {

        List<PmsSkuInfo> pmsSkuInfos= skuService.getAllSku();

        List<PmsSearchSkuInfo> pmsSearchSkuInfos=new ArrayList<>();

        for (PmsSkuInfo pmsSkuInfo:pmsSkuInfos){

            PmsSearchSkuInfo pmsSearchSkuInfo=new PmsSearchSkuInfo();

            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);

            pmsSearchSkuInfos.add(pmsSearchSkuInfo);

        }

        //将数据库的数据放入elasticsearch中
        /**
         * builder 数据
         * index 库名
         * type 表名
         * id 主键
         */
        for (PmsSearchSkuInfo pmsSearchSkuInfo:pmsSearchSkuInfos){

            Index put = new Index.Builder(pmsSearchSkuInfo).index("glmall").type("pmsSkuInfo").id(pmsSearchSkuInfo.getId()).build();

            jestClient.execute(put);
        }

    }


}
