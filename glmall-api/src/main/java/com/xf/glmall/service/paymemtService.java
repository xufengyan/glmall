package com.xf.glmall.service;

import com.xf.glmall.entity.PaymentInfo;

public interface paymemtService {
    int savePaymemtInfo(PaymentInfo paymentInfo);

    int updatePaymenInfo(PaymentInfo paymentInfo);
}
