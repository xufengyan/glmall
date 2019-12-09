package com.xf.glmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.xf.glmall.dao.paymemtMapper;
import com.xf.glmall.entity.PaymentInfo;
import com.xf.glmall.service.paymemtService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

@Service
public class paymemtServiceImpl implements paymemtService {

    @Autowired
    paymemtMapper paymemtMapper;

    @Override
    public int savePaymemtInfo(PaymentInfo paymentInfo) {
        int mag = paymemtMapper.insertSelective(paymentInfo);
        return mag;
    }

    @Override
    public int updatePaymenInfo(PaymentInfo paymentInfo) {

        Example example =new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());
        int mag = paymemtMapper.updateByExample(paymentInfo, example);
        return mag;
    }
}
