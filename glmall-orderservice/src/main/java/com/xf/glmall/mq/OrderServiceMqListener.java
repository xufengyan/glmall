package com.xf.glmall.mq;

import com.xf.glmall.entity.OmsOrder;
import com.xf.glmall.service.orderService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    orderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage){
        try {
            String orderSn = mapMessage.getString("orderSn");
            OmsOrder order =new OmsOrder();
            order.setOrderSn(orderSn);
            int mag = orderService.updateOrderbySn(order);
            System.out.println("订单修改哈");
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }


}
