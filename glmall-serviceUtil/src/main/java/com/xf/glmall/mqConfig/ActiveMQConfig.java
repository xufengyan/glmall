package com.xf.glmall.mqConfig;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;

@Configuration
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL ;

    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;

    @Bean
    public    ActiveMQUtil   getActiveMQUtil() throws JMSException {
        if(brokerURL.equals("disabled")){
            return null;
        }
        ActiveMQUtil activeMQUtil=new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return  activeMQUtil;
    }

    //定义一个消息监听器连接工厂，这里定义的是点对点模式的监听器连接工厂
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory ) {

        //将activeMQ的监听器封装进spring容器中
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        if(!listenerEnable.equals("true")){
            return null;
        }

        factory.setConnectionFactory(activeMQConnectionFactory);
        //设置并发数
        factory.setConcurrency("5");

        //重连间隔时间
       factory.setRecoveryInterval(5000L);
       factory.setSessionTransacted(false);
       factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        return factory;
    }


    /**
     * 死信队列消息处理
     * @return
     */
    @Bean
    public RedeliveryPolicy activeMQpolicy(){
        RedeliveryPolicy redeliveryPolicy =new RedeliveryPolicy();
        //是否在每次尝试发送失败后，增长这个等待时间
        redeliveryPolicy.setUseExponentialBackOff(true);
        //重发次数，默认6次，这里设置为1次
        redeliveryPolicy.setMaximumRedeliveries(6);
        //重发时间间隔，默认为1秒
        redeliveryPolicy.setInitialRedeliveryDelay(1000);
        //每次重发等待时间上*2
        redeliveryPolicy.setBackOffMultiplier(2);
        // 最大传送延迟，只在useExponentialBackOff为true时有效（V5.5），假设首次重连间隔为10ms，倍数为2，那么第
        //  二次重连时间间隔为 20ms，第三次重连时间间隔为40ms，当重连时间间隔大的最大重连时间间隔时，
        //  以后每次重连时间间隔都为最大重连时间间隔。
        redeliveryPolicy.setMaximumRedeliveryDelay(1000);
        redeliveryPolicy.setDestination(allDestination());
        return redeliveryPolicy;
    }


    @Bean
    public ActiveMQQueue allDestination(){
        return new ActiveMQQueue("C2S.Q.TEST");
    }



    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){
/*        if((url==null||url.equals(""))&&!brokerURL.equals("disabled")){
            url=brokerURL;
        }*/
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory(  brokerURL);
        return activeMQConnectionFactory;
    }

}