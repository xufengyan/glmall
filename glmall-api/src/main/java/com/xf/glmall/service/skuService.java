package com.xf.glmall.service;

import com.xf.glmall.entity.PmsSearchParam;
import com.xf.glmall.entity.PmsSearchSkuInfo;
import com.xf.glmall.entity.PmsSkuInfo;

import java.util.List;

public interface skuService{
    PmsSkuInfo getSkuById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> getAllSku();

    List<PmsSearchSkuInfo> getSearchSkuinfoByPmaram(PmsSearchParam pmsSearchParam);
}
