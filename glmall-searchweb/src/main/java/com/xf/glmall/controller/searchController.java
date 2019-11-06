package com.xf.glmall.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xf.glmall.entity.*;
import com.xf.glmall.service.attrService;
import com.xf.glmall.service.skuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;


@Controller
public class searchController {


    @Reference
    skuService skuService;

    @Reference
    attrService attrService;


    @RequestMapping("index.html")
    public String index() {
        return "index";
    }


    /**
     * 列表数据查询
     * @param pmsSearchParam
     * @param model
     * @return
     */
    @RequestMapping("list.html")
    public ModelAndView listHtml(PmsSearchParam pmsSearchParam, ModelAndView model) {
        List<PmsSearchSkuInfo> searchSkuInfos = skuService.getSearchSkuinfoByPmaram(pmsSearchParam);

        if (!searchSkuInfos.isEmpty()) {
            //从查询的数据中聚合平台属性
            Set<String> valueIdSet = new HashSet<>();
            for (PmsSearchSkuInfo pmsSearchSkuInfo : searchSkuInfos) {
                List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSearchSkuInfo.getPmsSkuAttrValueList();
                for (PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValues) {
                    valueIdSet.add(pmsSkuAttrValue.getValueId());
                }
            }

            //根据聚合的平台属性id查询的平台属性集合
            List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueIds(valueIdSet);

            //绑定已经选中的平台属性id到url上
            String urlParam = getUrlParam(pmsSearchParam);

            //通过迭代器删除已经点击的平台属性不在列表显示
            Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
            //面包屑集合
            List<searchCrumb> searchCrumbs = new ArrayList<>();

            if (pmsSearchParam.getValueId() != null) {
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo PmsBaseAttrInfo = iterator.next();
                    //遍历所有的的平台属性
                    for (PmsBaseAttrValue pmsBaseAttrValue : PmsBaseAttrInfo.getAttrValueList()) {
                        //遍历点击的平台属性
                        for (String valueId : pmsSearchParam.getValueId()) {
                            if (pmsBaseAttrValue.getId().equals(valueId)) {

                                //将点击的平台参数放入到面包屑中
                                searchCrumb searchCrumb=new searchCrumb();
                                searchCrumb.setValueId(valueId);
                                searchCrumb.setValueName(PmsBaseAttrInfo.getAttrName()+"："+pmsBaseAttrValue.getValueName());
                                searchCrumb.setUrlParam(getUrlParam(pmsSearchParam,valueId));
                                //删除当前元素
                                iterator.remove();
                                //添加对应valueId的面包屑
                                searchCrumbs.add(searchCrumb);
                            }
                        }
                    }
                }
                //有平台属性id值的时候才将面包屑传入前端
                model.addObject("attrValueSelectedList", searchCrumbs);
            }

            //如果关键字不为空，则将关键字传递到前台
            if (StringUtils.isNotBlank(pmsSearchParam.getKeyword())) {
                model.addObject("keyword", pmsSearchParam.getKeyword());
            }

            //前端面包屑
            model.addObject("attrList", pmsBaseAttrInfos);
            model.addObject("skuLsInfoList", searchSkuInfos);
            model.addObject("urlParam", urlParam);
            model.setViewName("list");
        } else {
            model.setViewName("404");
        }

        return model;
    }


    /**
     * 拼接点击了的平台属性的id到url上
     * @param pmsSearchParam
     * @return
     */
    private String getUrlParam(PmsSearchParam pmsSearchParam, String ...delValueId) {//String ...**表示可变行参数

        String urlParam = "";
        if (StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + pmsSearchParam.getCatalog3Id();
        }

        if (StringUtils.isNotBlank(pmsSearchParam.getKeyword())) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + pmsSearchParam.getKeyword();
        }


        if (pmsSearchParam.getValueId() != null) {

            for (String pmsSkuAttrValue : pmsSearchParam.getValueId()) {
                if (StringUtils.isNotBlank(pmsSkuAttrValue)) {
                    if(delValueId.length>0){
                        if(!delValueId[0].equals(pmsSkuAttrValue)){
                            if (StringUtils.isNotBlank(urlParam)) {
                                urlParam = urlParam + "&";
                            }
                            urlParam = urlParam + "valueId=" + pmsSkuAttrValue;
                        }
                    }else{
                        if (StringUtils.isNotBlank(urlParam)) {
                            urlParam = urlParam + "&";
                        }
                        urlParam = urlParam + "valueId=" + pmsSkuAttrValue;
                    }
                }

            }

        }
        return urlParam;
    }


}
