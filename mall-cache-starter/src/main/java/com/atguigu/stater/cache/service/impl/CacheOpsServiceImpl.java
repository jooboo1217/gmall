package com.atguigu.stater.cache.service.impl;


import com.atguigu.stater.cache.constant.SysRedisConst;

import com.atguigu.stater.cache.service.CacheOpsService;
import com.atguigu.stater.cache.utils.Jsons;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.lang.reflect.Type;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CacheOpsServiceImpl implements CacheOpsService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Resource
    RedissonClient redissonClient;
    
    //定时的线程池
    ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
    
    //从缓存中查询
    @Override
    public <T>T getCacheData(String cacheKey, Class<T> clz) {
        String cache = redisTemplate.opsForValue().get(cacheKey);
        Gson gson = new Gson();
        if (SysRedisConst.NULL_VAL.equals(cache)){
            return null;
            
        }
        //T t = Jsons.toObj(cache,clz);
        T t = gson.fromJson(cache, clz);
        return t;
    }


    //获取缓存中的数据，并转换为需要的对象类型
    @Override
    public Object getCacheData(String cacheKey, Type type) {
        String jsonStr = redisTemplate.opsForValue().get(cacheKey);
        //判断查出来的值是不是null,如果是null值，直接反击null
        if (SysRedisConst.NULL_VAL.equals(jsonStr)){
            return null;
        }
        //逆转json为Type类型的复杂对象
        Object obj = Jsons.toObj(jsonStr, new TypeReference<Object>() {
            @Override
            public Type getType() {
                return type; //这个是方法的带泛型的返回值类型
            }
        });
        return obj;
    }

    //从布隆过滤器中查询
    @Override
    public boolean bloomContains(Object skuId) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(SysRedisConst.BLOOM_SKUID);
        return bloomFilter.contains(skuId);
    }
    //查看布隆过滤器中是否存在该对象
    @Override
    public boolean bloomContains(String bloomName, Object bVal) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(bloomName);
        boolean contains = bloomFilter.contains(bVal);
        return contains;
    }

    //加锁
    @Override
    public boolean tryLock(Long skuId) {
        //1.锁也是redisson中创建的，准备锁的key
        String lockKey = SysRedisConst.LOCK_SKU_DETAIL + skuId;
        //2.创建锁
        RLock lock = redissonClient.getLock(lockKey);
        //3.尝试加锁
        boolean b = lock.tryLock();
        return b;
    }

    //尝试加锁
    @Override
    public boolean tryLock(String lockName) {
        RLock lock = redissonClient.getLock(lockName);
        return lock.tryLock();
    }


    //将回源回来的数据存储到缓存中，可能会有null，存储的时间短一些就好
    @Override
    public void saveData(String cacheKey, Object skuDetailFromRpc) {
        //判断查到的数据是否为null
        if (skuDetailFromRpc == null){
            //null值也储存，就是存储的时间短一些
            redisTemplate.opsForValue()
                    .set(cacheKey,
                            SysRedisConst.NULL_VAL,
                            SysRedisConst.NULL_VAL_TTL,
                            TimeUnit.SECONDS);
        }else {
//            Gson gson = new Gson();
//            String skuDetail = gson.toJson(skuDetailFromRpc);
            String skuDetail = Jsons.toStr(skuDetailFromRpc);
            redisTemplate.opsForValue().set(cacheKey,
                                    skuDetail,
                                    SysRedisConst.SKUDETAIL_TTL,
                                    TimeUnit.SECONDS);
        }
    }
    //解锁
    @Override
    public void unlock(Long skuId) {
        //获得这把锁的key
        String locKey = SysRedisConst.LOCK_SKU_DETAIL + skuId;
        //获得这把锁
        RLock lock = redissonClient.getLock(locKey);
        //关锁
        lock.unlock();
    }


    //解锁
    @Override
    public void unlock(String lockName) {
        RLock lock = redissonClient.getLock(lockName);
        lock.unlock();
    }

    /**
     * 延迟双删
     * @param cacheKey
     */
    @Override
    public void delay2Delete(String cacheKey) {
        //删除缓存中的数据
        redisTemplate.delete(cacheKey);
        //设置一个定时任务，过了一段时间再进行一次删除，不能使用睡得方式，在获取线程池的四种方法中有个定时的线程池
        scheduledExecutor.schedule(()->{
            redisTemplate.delete(cacheKey);
        },5,TimeUnit.SECONDS);
    }
}
