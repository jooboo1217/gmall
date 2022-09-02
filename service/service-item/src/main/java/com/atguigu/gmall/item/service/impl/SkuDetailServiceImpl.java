package com.atguigu.gmall.item.service.impl;


import com.atguigu.gmall.common.result.Result;


import com.atguigu.gmall.feign.product.SkuProductFeignClient;
import com.atguigu.gmall.item.service.SkuDetailService;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import com.atguigu.stater.cache.annotation.GmallCache;
import com.atguigu.stater.cache.constant.SysRedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class SkuDetailServiceImpl implements SkuDetailService {
    /*  @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        SkuDetailTo detailTo = new SkuDetailTo();
        //远程调用商品服务查询
        Result<SkuDetailTo> skuDetail = skuDetailFeignClient.getSkuDetail(skuId);

        return skuDetail.getData();
    }*/

    //
    @Autowired
    SkuProductFeignClient skuDetailFeignClient;

    @Resource
    ThreadPoolExecutor executor;





    public SkuDetailTo getSkuDetailFromRpc(Long skuId) {
        SkuDetailTo detailTo = new SkuDetailTo();

        //使用异步编排，每一个查询都使用一个线程，可以提高查询的效率

        //查询skuInfo,有返回值，返回skuInfo,后面查询需要用到
        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            Result<SkuInfo> result = skuDetailFeignClient.getSkuInfo(skuId);
            SkuInfo skuInfo = result.getData();
            detailTo.setSkuInfo(skuInfo);
            return skuInfo;
        }, executor);


        //查询skuImage需要skuId,需要使用查出来的skuInfo,但是不需要再返回值
        CompletableFuture<Void> imageFuture = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            if (skuInfo != null){
                Result<List<SkuImage>> skuImages = skuDetailFeignClient.getSkuImages(skuId);
                List<SkuImage> images = skuImages.getData();
                skuInfo.setSkuImageList(images);
            }

        }, executor);


        //查询实时价格，不需要接收值，也不需要返回值，直接查询
        CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
            Result<BigDecimal> sku1010Price = skuDetailFeignClient.getSku1010Price(skuId);
            BigDecimal price = sku1010Price.getData();
            detailTo.setPrice(price);
        }, executor);


        //查询销售属性值,需要使用查询出来的skuInf,不需要返回值
        CompletableFuture saleAttrFuture = skuInfoFuture.thenAcceptAsync((skuInfo)->{
            if (skuInfo != null){
                Long spuId = skuInfo.getSpuId();
                Result<List<SpuSaleAttr>> saleAndAttr  = skuDetailFeignClient.getSkuSaleattrvalues(skuId, spuId);
                List<SpuSaleAttr> skuSaleattrvalues = saleAndAttr.getData();
                detailTo.setSpuSaleAttrList(skuSaleattrvalues);
            }
        },executor);


        //通过三级分类，查询一二级分类,需要skuInfo,不需要返回值
        CompletableFuture cateGoryFuture =  skuInfoFuture.thenAcceptAsync((skuInfo)->{
            if (skuInfo != null){
                Long category3Id = skuInfo.getCategory3Id();
                Result<CategoryViewTo> categoryViews = skuDetailFeignClient.getCategoryView(category3Id);
                CategoryViewTo categoryViewTo = categoryViews.getData();
                detailTo.setCategoryView(categoryViewTo);
            }

        },executor);

        //查询valueJson，键值对组合,需要skuInfo,不需要返回值
        CompletableFuture skuVlaueFuture =  skuInfoFuture.thenAcceptAsync((skuInfo)->{
            if (skuInfo != null){
                Result<String> sKuValueJsons = skuDetailFeignClient.getSKuValueJson(skuInfo.getSpuId());
                String sKuValueJson = sKuValueJsons.getData();
                detailTo.setValuesSkuJson(sKuValueJson);
            }
        },executor);

        //在所有的线程，数据都查询完毕了，再返回
        CompletableFuture.allOf(skuInfoFuture,imageFuture,priceFuture,saleAttrFuture
                ,cateGoryFuture,skuVlaueFuture).join();


        return detailTo;
    }

   

    //缓存优化一： 将查询到的数据缓存到本地
  /*  @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        //创建一个map做本地缓存
        HashMap<Long, SkuDetailTo> skuCache = new HashMap<>();
        //从数据库取缓存
        SkuDetailTo  cache = skuCache.get(skuId);
        //判断是否存在缓存,不存在就去数据库查,然后存储到缓存中
        if (cache==null){
            //调动查询数据库的方法
            SkuDetailTo skuDetailFromRpc = getSkuDetailFromRpc(skuId);
            //将查询到的存储到map中
            skuCache.put(skuId,skuDetailFromRpc);
            return skuDetailFromRpc;
        }
        //缓存中有，直接返回缓存
        return cache;
    }*/

    //缓存优化二：将查询到的数据缓存到Redis中
/*
    @Autowired
    CacheOpsService cacheOpsService;
    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        //创建一个常量类
        //获取缓存key
        String cacheKey = SysRedisConst.SKU_INFO_PREFIX +skuId;

        //1.浏览器的请求过来，先到缓存中拿
        SkuDetailTo cacheData = cacheOpsService.getCacheData(cacheKey,SkuDetailTo.class);
        //2.判断,是否命中缓存，如果拿到了，直接返回，如果没拿到，就要到"布隆过滤器查看"
        if (cacheData == null){//3.存中没有
            //调用布隆过滤器查看
            boolean contain =  cacheOpsService.bloomContains(skuId);
            //4.判断布隆过滤器返回值
            if (!contain){
                //5.不存在，布隆说没有就一定乜有，直接返回null
                return null;
            }
            //6.说明布隆显示可能有这个数据，先加锁，然后去数据库回源
           boolean lock = cacheOpsService.tryLock(skuId); //加锁
            //判断是否加锁成功
            if (lock){
                //7.加锁成功，回源
                SkuDetailTo skuDetailFromRpc = getSkuDetailFromRpc(skuId);
                //8.将查询到的数据存储到缓存中，返回
                cacheOpsService.saveData(cacheKey,skuDetailFromRpc);
                //9.解锁
                cacheOpsService.unlock(skuId);
                //返回值
                return skuDetailFromRpc;
            }
            //10.没有拿到锁的，等上一秒钟，从缓存中拿，拿到锁的已经放到缓存中了
            try {
                Thread.sleep(1000);
                return cacheOpsService.getCacheData(cacheKey,SkuDetailTo.class);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //说明拿到的缓存，
        return cacheData;
    }*/

    /**
     * 优化三 ：  将从缓存中拿数据的方法提取为AOP
     * @param skuId
     * @return
     */
    @GmallCache(cacheKey = SysRedisConst.SKU_INFO_PREFIX+"#{#params[0]}",
                bloomName = SysRedisConst.BLOOM_SKUID,
                bloomValue = "#{#params[0]}",
                lockName = SysRedisConst.LOCK_SKU_DETAIL+"#{#params[0]}"
    )
    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        SkuDetailTo skuDetailFromRpc = getSkuDetailFromRpc(skuId);
        return skuDetailFromRpc;
    }
}
