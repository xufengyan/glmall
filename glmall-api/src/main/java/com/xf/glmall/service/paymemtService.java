package com.xf.glmall.service;

import com.xf.glmall.entity.PaymentInfo;

import java.util.Map;

public interface paymemtService {
    int savePaymemtInfo(PaymentInfo paymentInfo);

    int updatePaymenInfo(PaymentInfo paymentInfo);

    void sendDelaPaymentResult(String orderSn,int count);

    Map<String, Object> checkedAlipayPayment(String orderSn);
}
