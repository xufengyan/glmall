package com.xf.glmall.service;



import com.xf.glmall.entity.PmsProductImage;
import com.xf.glmall.entity.PmsProductInfo;
import com.xf.glmall.entity.PmsProductSaleAttr;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);

    void saveSpuInfo(PmsProductInfo pmsProductInfo);

    List<PmsProductSaleAttr> spuSaleAttrList(String spuId);

    List<PmsProductImage> spuImageList(String spuId);

    List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId, String skuId);
}
