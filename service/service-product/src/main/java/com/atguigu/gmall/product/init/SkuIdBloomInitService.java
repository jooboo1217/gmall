package com.atguigu.gmall.product.init;



/*@Service
@Slf4j
public class SkuIdBloomInitService {

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    RedissonClient redissonClient;

    @PostConstruct  //当前组件对象创建成功以后
    public void initSkuBloom(){
        log.info("布隆过滤器开始");
        //1.查询出所有的商品id
        List<Long> skuIds =  skuInfoService.findAllSkuId();
        //2.查询出boomfilter过滤器
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(SysRedisConst.BLOOM_SKUID);
        //3.初始化过滤器//long expectedInsertions, 期望插入的数据量
                     //double falseProbability  误判率
        boolean exists = bloomFilter.isExists();
        if (!exists){
            bloomFilter.tryInit(5000000,0.00001);
        }
        //4.遍历所有的商品id保存布隆中
        for (Long skuId : skuIds) {
            //System.out.println("skuId = " + skuId);
            bloomFilter.add(skuId);
        }

        log.info("布隆初始化完成....，总计添加了 {} 条数据",skuIds.size());
    }
}*/


import com.atguigu.gmall.product.service.SkuInfoService;
import com.atguigu.stater.cache.constant.SysRedisConst;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class SkuIdBloomInitService {

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    RedissonClient redissonClient;


    //TODO 布隆只能增，不能删除商品，如果真的数据库删除了商品，布隆怎么办？
    //      布隆重建。

    /**
     * 项目一启动就运行
     */
    @PostConstruct  //当前组件对象创建成功以后
    public void initSkuBloom(){
        log.info("布隆初始化正在进行....");
        //1、查询出所有的skuId
        List<Long> skuIds  = skuInfoService.findAllSkuId();
        /*List<Integer> skuIds  = new ArrayList<>();
        skuIds.add(1);
        skuIds.add(2);
        skuIds.add(3);
        skuIds.add(4);*/

        //2、把所有的id初始化到布隆过滤器中
        RBloomFilter<Object> filter =
                redissonClient.getBloomFilter(SysRedisConst.BLOOM_SKUID);


        //3、初始化布隆过滤器
        //long expectedInsertions, 期望插入的数据量
        //double falseProbability  误判率
        boolean exists = filter.isExists();
        if(!exists){
            //尝试初始化。如果布隆过滤器没有初始化过，就尝试初始化
            filter.tryInit(5000000,0.00001);
            //
        }


        //4、把所有的商品添加到布隆中。 不害怕某个微服务把这个事情做失败
        for (Long skuId : skuIds) {
            System.out.println("skuId = " + skuId);
            filter.add(skuId);
        }



        log.info("布隆初始化完成....，总计添加了 {} 条数据",skuIds.size());
        //
//        filter.contains(10L);

    }
}
