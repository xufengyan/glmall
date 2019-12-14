package com.xf.glmall.mq;

import com.xf.glmall.entity.PaymentInfo;
import com.xf.glmall.service.paymemtService;
import org.apache.tomcat.util.security.Escape;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Map;


/**
 * 查询支付状态监听器
 */
@Component
public class paymentMqListener {

    @Autowired
    paymemtService paymemtService;

    @JmsListener(destination = "PAYMENT_CHECKED_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentCheckedResult(MapMessage mapMessage){

        try {
            System.out.println("进入支付状态查询监听器");

            String orderSn = mapMessage.getString("orderSn");
            Integer count = Integer.parseInt(mapMessage.getString("count"));

            Map<String,Object> resultPay = paymemtService.checkedAlipayPayment(orderSn);


            if(null != resultPay && !resultPay.isEmpty()){
                String tradeStatus = (String) resultPay.get("trade_status");
                if("TRADE_SUCCESS".equals(tradeStatus)){
                    PaymentInfo paymentInfo =new PaymentInfo();
                    paymentInfo.setOrderSn(orderSn);
                    paymentInfo.setAlipayTradeNo((String)resultPay.get("trade_no"));
                    paymentInfo.setPaymentStatus("已支付");
                    paymentInfo.setCallbackContent((String)resultPay.get("call_back_content"));
                    paymentInfo.setCallbackTime(new Date());
                    paymentInfo.setSubject((String)resultPay.get("subject"));
                    paymemtService.updatePaymenInfo(paymentInfo);
                    System.out.println("已支付成功");

                    return;
                }
                //为了看一下状态的，可有可无
                switch (tradeStatus){
//                case "WAIT_BUYER_PAY":
//                    //未付款，重新发送消息队列
//                    if(count>0){
//                        System.out.println("当前检查次数为"+count);
//                        paymemtService.sendDelaPaymentResult(orderSn,count);
//                        count--;
//                        System.out.println("交易创建，等待买家付款");
//                    }else{
//                        System.out.println("检查次数用尽，结束检查");
//                    }
//                    break;
                    case "TRADE_CLOSED":
                        System.out.println("未付款交易超时关闭，或支付完成后全额退款");
                        break;
                    case "TRADE_SUCCESS":
                        System.out.println("交易支付成功");
                        break;
                    case "TRADE_FINISHED":
                        System.out.println("交易结束，不可退款");
                        break;
                }
            }

            //未付款，重新发送消息队列
            if(count>0){
                System.out.println("当前检查次数为"+count);
                count--;
                paymemtService.sendDelaPaymentResult(orderSn,count);
                System.out.println("交易创建，等待买家付款");
            }else{
                System.out.println("检查次数用尽，结束检查");
            }




        } catch (JMSException e) {
            e.printStackTrace();
        }


    }


}
