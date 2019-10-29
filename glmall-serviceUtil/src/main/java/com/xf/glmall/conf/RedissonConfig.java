package com.xf.glmall.conf;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:disabled}")
    private String host="192.168.222.20";

    @Value("${spring.redis.port:0}")
    private String port = "6179";

    @Value("${spring.redis.password:disabled}")
    private String password="123456";
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port).setPassword(password);

        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}