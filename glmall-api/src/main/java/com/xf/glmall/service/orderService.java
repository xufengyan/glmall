package com.xf.glmall.service;

import com.xf.glmall.entity.OmsOrder;

import java.math.BigDecimal;

public interface orderService {
    Boolean checkTradeCode(String memberId, String proidSum,String tradeCode);

    String gentradeCode(String memberId, String proidSum);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByorderSn(OmsOrder order);

    int updateOrderbySn(OmsOrder order);
}
