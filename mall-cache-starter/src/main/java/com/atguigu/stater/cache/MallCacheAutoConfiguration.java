package com.atguigu.stater.cache;

import com.atguigu.stater.cache.aspect.CacheAspect;
import com.atguigu.stater.cache.service.CacheOpsService;
import com.atguigu.stater.cache.service.impl.CacheOpsServiceImpl;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


import javax.annotation.Resource;

@EnableAspectJAutoProxy
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class MallCacheAutoConfiguration {

    @Autowired
    RedisProperties redisProperties;
    
    @Bean
    public CacheAspect cacheAspect(){
        return new CacheAspect();
    }

    @Bean
    public CacheOpsService cacheOpsService(){
        return  new CacheOpsServiceImpl();
    }

    @Bean
    public RedissonClient redissonClient(){
        //1.创建配置
        Config config = new Config();
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String password = redisProperties.getPassword();
        //2.设置redisson的配置项
        config.useSingleServer()
                .setAddress("redis://"+host+":"+port)
                .setPassword(password);

        //3.创建一个ResissonClient
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
