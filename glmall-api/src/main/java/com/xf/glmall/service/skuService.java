package com.xf.glmall.service;

import com.xf.glmall.entity.OmsCartItem;
import com.xf.glmall.entity.PmsSearchParam;
import com.xf.glmall.entity.PmsSearchSkuInfo;
import com.xf.glmall.entity.PmsSkuInfo;

import java.math.BigDecimal;
import java.util.List;

public interface skuService{

    //查询商品信息业务------------------------------------
    PmsSkuInfo getSkuById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> getAllSku();

    List<PmsSearchSkuInfo> getSearchSkuinfoByPmaram(PmsSearchParam pmsSearchParam);


    //购物车业务-----------------------------------------

    OmsCartItem ifCartExistByUser(String menberId, String skuId);

    int updateCart(OmsCartItem omsCartItemFromDb);

    int addCart(OmsCartItem omsCartItem);

    List<OmsCartItem> flushCartCahe(String menberId);

    List<OmsCartItem> cartList(String userId);

    int checkCart(OmsCartItem omsCartItem);

    Boolean checkPrice(String productId, BigDecimal productPrice);

    int delCartById(String prodoctId);
}
