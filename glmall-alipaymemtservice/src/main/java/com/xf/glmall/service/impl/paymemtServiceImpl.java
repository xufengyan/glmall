package com.xf.glmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.sun.org.apache.regexp.internal.RE;
import com.xf.glmall.dao.paymemtMapper;
import com.xf.glmall.entity.PaymentInfo;
import com.xf.glmall.mqConfig.ActiveMQUtil;
import com.xf.glmall.service.paymemtService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class paymemtServiceImpl implements paymemtService {

    @Autowired
    paymemtMapper paymemtMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public int savePaymemtInfo(PaymentInfo paymentInfo) {
        int mag = paymemtMapper.insertSelective(paymentInfo);
        return mag;
    }

    @Override
    public int updatePaymenInfo(PaymentInfo paymentInfo) {

//        幂等性检查
        //对于同一订单修改，只允许修改一次
        PaymentInfo pay =new PaymentInfo();
        pay.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymemtMapper.selectOne(pay);

        if(StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus())&&"已支付".equals(paymentInfoResult.getPaymentStatus())){
            return 1;
        }

        int mag = 0;
        Example example =new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());
        Connection connection =null;
        Session session = null;
        //创建activeMQ
        //发送订单修改消息
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        try {
            //对支付信息进行修改
            mag = paymemtMapper.updateByExample(paymentInfo, example);

//            调用mq发送支付成功数据修改的消息
            Queue payment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            //普通字符串类型消息
//            TextMessage textMessage =new ActiveMQTextMessage();
            //hash类型消息
            MapMessage mapMessage =new ActiveMQMapMessage();
            mapMessage.setString("orderSn",paymentInfo.getOrderSn());
            producer.send(mapMessage);
            session.commit();
        }catch (Exception e){
            //如果修改出现异常就回滚
            try {
                session.rollback();
                System.out.println("支付修改失败，开始回滚");
            } catch (JMSException e1) {
                e1.printStackTrace();
            }

        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        return mag;
    }

    /**
     * 发送检查支付的消息队列
     * @param orderSn
     * @param count
     */
    @Override
    public void sendDelaPaymentResult(String orderSn,int count) {

        Connection connection =null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        try {
//            调用mq发送支付成功数据修改的消息
            Queue payment_success_queue = session.createQueue("PAYMENT_CHECKED_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            //普通字符串类型消息
//            TextMessage textMessage =new ActiveMQTextMessage();
            //hash类型消息
            MapMessage mapMessage =new ActiveMQMapMessage();
            mapMessage.setString("orderSn",orderSn);
            mapMessage.setInt("count",count);
            //为消息加入延时时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*30);
            producer.send(mapMessage);
            session.commit();
        }catch (Exception e){
            //如果修改出现异常就回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }

        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }





    }

    /**
     * 检查支付状态
     * @param orderSn
     * @return
     */
    @Override
    public Map<String, Object> checkedAlipayPayment(String orderSn) {

        AlipayTradeQueryRequest alirequest = new AlipayTradeQueryRequest();
        Map<String,Object> aliMap=new HashMap<>();
        aliMap.put("out_trade_no",orderSn);
        alirequest.setBizContent(JSON.toJSONString(aliMap));
        AlipayTradeQueryResponse aliResponse = null;
        Map<String,Object> responseMap=new HashMap<>();

        try {
            aliResponse = alipayClient.execute(alirequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(aliResponse.isSuccess()){
            System.out.println("调用成功");
            responseMap.put("out_trade_no",aliResponse.getOutTradeNo());
            responseMap.put("trade_no",aliResponse.getTradeNo());
            responseMap.put("trade_status",aliResponse.getTradeStatus());
            responseMap.put("call_back_content",aliResponse.getMsg());
            responseMap.put("subject",aliResponse.getSubMsg());
            return responseMap;
        }else {
            System.out.println("调用失败");
            return null;
        }


    }
}
