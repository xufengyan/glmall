package com.xf.glmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.xf.glmall.dao.paymemtMapper;
import com.xf.glmall.entity.PaymentInfo;
import com.xf.glmall.service.paymemtService;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class paymemtServiceImpl implements paymemtService {

    @Autowired
    paymemtMapper paymemtMapper;

    @Override
    public int savePaymemtInfo(PaymentInfo paymentInfo) {
        int mag = paymemtMapper.insertSelective(paymentInfo);
        return mag;
    }
}
