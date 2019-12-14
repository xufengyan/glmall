package com.xf.glmall.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.xf.glmall.Util.RedisUtil;
import com.xf.glmall.dao.omsOrderItemMapper;
import com.xf.glmall.dao.omsOrderMapper;
import com.xf.glmall.entity.OmsOrder;
import com.xf.glmall.entity.OmsOrderItem;
import com.xf.glmall.mqConfig.ActiveMQUtil;
import com.xf.glmall.service.orderService;
import com.xf.glmall.service.skuService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;


import javax.jms.*;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class orderServiceImpl implements orderService {


    @Autowired
    RedisUtil redisUtil;
    @Autowired
    omsOrderMapper omsOrderMapper;
    @Autowired
    omsOrderItemMapper omsOrderItemMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;


    @Reference
    skuService skuService;

    @Override
    public Boolean checkTradeCode(String memberId, String proidSum,String tradeCod) {

        Jedis jedis= null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey="user:"+memberId+":proid:"+proidSum+":code";
            //使用lua脚本在发现key的时候删除，防止并发的时候订单多次提交
            //KEYS[1](redis中保存数据的key)
            //ARGC[1] 表示要跟redis中对比的数据
            //当redis中数据和传入的数据一致的时候，则将这个key对应的值删除
            //不一致就返回0
            String orderScript = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(orderScript, Collections.singletonList(tradeKey),
                    Collections.singletonList(tradeCod));
            //判断脚本执行完后的返回值
            if(eval!=null&&eval!=0){
                return true;
            }
        }catch (Exception e){
            System.out.println(e);
        }finally {
            jedis.close();
        }
        return false;
    }

    @Override
    public String gentradeCode(String memberId, String proidSum) {
        Jedis jedis =null;
        String tradeCode =null;
        try {

            jedis=redisUtil.getJedis();

            String tradeKey = "user:"+memberId+":proid:"+proidSum+":code";
            //通过商品id拼接生成随机数，并放入redis中
            tradeCode= proidSum + UUID.randomUUID().toString();

            jedis.setex(tradeKey,60*15,tradeCode);

        }catch (Exception e){
            System.out.println(e);
        }finally {
            jedis.close();
        }

        return tradeCode;
    }


    @Override
    public void saveOrder(OmsOrder omsOrder) {

        //保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId=omsOrder.getId();
        //保存订单详情表
        List<OmsOrderItem> omsOrderItems =omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            skuService.delCartById(omsOrderItem.getProductId());
        }


    }

    @Override
    public OmsOrder getOrderByorderSn(OmsOrder order) {

        OmsOrder omsOrder = omsOrderMapper.selectOne(order);

        return omsOrder;
    }

    @Override
    public int updateOrderbySn(OmsOrder order) {

        Example example =new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",order.getOrderSn());
        order.setStatus(1);
        Connection connection =null;
        Session session = null;
        int mag = 0;
        //创建activeMQ
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        try {
            mag = omsOrderMapper.updateByExampleSelective(order, example);

//            调用mq发送支付成功数据修改的消息
            //发送库存消费消息
            Queue payment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            //普通字符串类型消息
//            TextMessage textMessage =new ActiveMQTextMessage();
            //hash类型消息
            MapMessage mapMessage =new ActiveMQMapMessage();
//            mapMessage.setString("orderSn",paymentInfo.getOrderSn());
            producer.send(mapMessage);
            session.commit();
        }catch (Exception e){
            //如果修改出现异常就回滚
            try {
                session.rollback();
                System.out.println("订单修改失败，开始回滚");
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
}
