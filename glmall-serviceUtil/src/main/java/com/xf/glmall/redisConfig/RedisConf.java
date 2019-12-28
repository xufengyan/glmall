package com.xf.glmall.redisConfig;

import com.xf.glmall.Util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConf {
    //读取配置文件中的redis的ip地址
    @Value("${spring.redis.host:disabled}")
    private String host = "127.0.0.1";
    @Value("${spring.redis.port:0}")
    private int port = 6379;
    @Value("${spring.redis.database:0}")
    private int database;
    @Value("${spring.redis.password:disabled}")
    private String password="123456";
        @Bean
        public RedisUtil getRedisUtil(){
            if(host.equals("disabled")){
                return null;
            }
            RedisUtil redisUtil=new RedisUtil();
            redisUtil.initPool(host,port,database,password);
            return redisUtil;
        }
}
