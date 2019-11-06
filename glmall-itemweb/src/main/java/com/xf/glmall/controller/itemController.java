package com.xf.glmall.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.xf.glmall.entity.*;
import com.xf.glmall.service.SpuService;
import com.xf.glmall.service.skuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class itemController {

    @Reference
    skuService skuService;
    @Reference
    SpuService spuService;


    /**
     *
     * @param model
     * @return
     */
    @RequestMapping("item.index")
    public String index(Model model){

        List<Integer> list=new ArrayList<>();

        for(int i=1;i<10;i++){
            list.add(i);
        }
        model.addAttribute("test","我只是来做测试的，我太难了");
        model.addAttribute("list",list);
        model.addAttribute("check",1);
        return "index";
    }





    @RequestMapping("{skuId}.html")
    public String itemHtml(@PathVariable String skuId,Model model){


        //查询商品信息
        PmsSkuInfo pmsSkuInfo=skuService.getSkuById(skuId);
        model.addAttribute("skuInfo",pmsSkuInfo);

        //查询商品属性
        List<PmsProductSaleAttr> pmsProductSaleAttrs= spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),skuId);
        model.addAttribute("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        HashMap<String, String> skuSaleAttrMap = new HashMap<>();
        //查询商品属性
        List<PmsSkuInfo> pmsSkuInfos=skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());

        //将商品属性保存为k(商品属性ID)-value（商品id）的格式
        for(PmsSkuInfo skuInfo:pmsSkuInfos){
            String k="";
            String v=skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValues=skuInfo.getPmsSkuSaleAttrValueList();

            for(int i=0;i<skuSaleAttrValues.size();i++){
                if(i!=0){
                    k+="|"+skuSaleAttrValues.get(i).getSaleAttrValueId();
                }else{
                    k=skuSaleAttrValues.get(i).getSaleAttrValueId();
                }
            }
            skuSaleAttrMap.put(k,v);
        }

        String skuSaleAttrMapJsonstr = JSON.toJSONString(skuSaleAttrMap);

        model.addAttribute("skuSaleAttrMapJsonstr",skuSaleAttrMapJsonstr);

        return "item";
    }


}
