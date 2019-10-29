package com.xf.glmall.RedissionTest;


import com.xf.glmall.Util.RedisUtil;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@RequestMapping("RedissionTest")
@Controller
public class RedissionTest {

    private int count=1;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;


    @RequestMapping("index")
    @ResponseBody
    public String getRedission(){


        Jedis jedis=redisUtil.getJedis();

        RLock rLock = redissonClient.getLock("lock");//声明redission锁
        rLock.lock();//上锁

        try {
            count++;
            System.out.println("测试值："+count);

        }finally {
            jedis.close();
            rLock.unlock();
        }

        return "测试中";
    }




}
