package com.atguigu.gmall.product.bloom.impl;

import com.atguigu.gmall.product.bloom.BloomDataQueryService;
import com.atguigu.gmall.product.bloom.RebuildBloom;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RebuildBloomIml implements RebuildBloom {


    @Autowired
    RedissonClient redissonClient;//创建或者根据布隆名称获得布隆过滤器

    @Autowired
    BloomDataQueryService bloomDataQueryService;//查询商品的所有id
    //重建布隆过滤器
    @Override
    public void rebuildBloom(String bloomName, BloomDataQueryService bloomDataQueryService) {
        //通过布隆过滤器的名称获得布隆过滤器,听过redissonClient获得
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(bloomName);
        //创建一个新的布隆过滤器
        //新名称
        String newBloomName = bloomName + "_new";
        RBloomFilter<Object> newBloomFilter = redissonClient.getBloomFilter(newBloomName);
        //初始化布隆过滤器
        newBloomFilter.tryInit(5000000,0.00001);
        //查询数据库中的商品最新的id
        List list = bloomDataQueryService.queryData();
        //遍历查询到的所有的id集合，添加到新的布隆过滤器中
        for (Object id : list) {
            newBloomFilter.add(id);
        }
        //使用一个中间值对布隆进行替换
        bloomFilter.rename("old");//将旧布隆的下线
        newBloomFilter.rename(bloomName);//新布隆上线
        //讲究的布隆过滤器删除
        bloomFilter.deleteAsync();//删除旧布隆
        redissonClient.getBloomFilter("old").deleteAsync();//删除中间的布隆
    }
}
