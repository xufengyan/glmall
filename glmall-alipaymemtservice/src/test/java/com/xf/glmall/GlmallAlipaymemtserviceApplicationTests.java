package com.xf.glmall;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.jms.*;

@SpringBootTest
class GlmallAlipaymemtserviceApplicationTests {

    @Test
    void contextLoads() {


    }

    public static void main(String[] args) {
        Connection connection = null;
        Session session = null;
        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://192.168.161.128:61616");
        try {
            connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue testqueue = session.createQueue("TEST2");

            MessageProducer producer = session.createProducer(testqueue);
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("今天天气真好！");
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            System.out.println("?????");
            System.out.println( 1 / 0);
            producer.send(textMessage);
            session.commit();

        } catch (Exception e) {
            try {
                session.rollback();
                System.out.println("回滚");
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }


//

    }





}
