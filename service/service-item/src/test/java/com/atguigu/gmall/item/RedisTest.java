package com.atguigu.gmall.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisTest {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void redisTest(){
        redisTemplate.opsForValue().set("hello","world");
        System.out.println("redis存入hello");

        String hello = redisTemplate.opsForValue().get("hello");
        System.out.println("hello = " + hello);

    }
}
