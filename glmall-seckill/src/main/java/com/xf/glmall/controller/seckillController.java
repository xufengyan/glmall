package com.xf.glmall.controller;

import com.xf.glmall.Util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class seckillController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;


    /**
     * 使用redission
     * @return
     */
    @RequestMapping("seckillTest2.html")
    @ResponseBody
    public String seckillTest2(){

        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            RSemaphore semaphore = redissonClient.getSemaphore("104");
            Boolean bool = semaphore.tryAcquire();

            Integer stock = Integer.parseInt(jedis.get("104"));

            if(bool){
                System.out.println("redission---当前库存量为："+(stock-1)+",当前成功抢购的人数为："+(10000-stock));
            }else{
                System.out.println("没有抢到，尴尬了");

            }
        }catch (Exception e){
            System.out.println(e);
        }finally {
            jedis.close();
        }
        return "2";
    }




    /**
     * 使用redis事务来实现秒杀
     * @return
     */
    @RequestMapping("seckillTest.html")
    @ResponseBody
    public String seckillTest(){
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            //开启redis监控
            jedis.watch("104");
            Integer stock = Integer.parseInt(jedis.get("104"));
            if(stock>0){
                //multi开始事务
                Transaction multi = jedis.multi();
                //对当前的key对应的值减1
                multi.incrBy("104",-1);
                //exec对于与warch监控的值是不是一致，一致的画就执行，
                // 不一致将不会执行当前事务中的命令
                List<Object> exec = multi.exec();

                if(exec!=null&&exec.size()>0){
                    System.out.println("redis----当前库存量为："+(stock-1)+",当前成功抢购的人数为："+(10000-stock));
                }else {
                    System.out.println("没有抢到，尴尬了");
                }
            }
        }catch (Exception e){
            System.out.println(e);

        }finally {
            jedis.close();
        }

        return "1";
    }


}
