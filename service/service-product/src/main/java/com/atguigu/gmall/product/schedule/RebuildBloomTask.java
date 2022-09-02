package com.atguigu.gmall.product.schedule;



import com.atguigu.gmall.product.bloom.BloomDataQueryService;
import com.atguigu.gmall.product.bloom.RebuildBloom;
import com.atguigu.stater.cache.constant.SysRedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

public class RebuildBloomTask {

    @Autowired
    RebuildBloom rebuildBlooml;
    @Autowired
    BloomDataQueryService bloomDataQueryService;
    //设置一个定时任务，对重建布隆的任务进行定时,springboot中内嵌scheduling

    @Scheduled(cron = "0 0 3 ? * 3")//秒 分 时 日 月 周  -->每周周三的早上三点整
    public void rebuilBloom(){
        //调用重建布隆的方法
        rebuildBlooml.rebuildBloom(SysRedisConst.BLOOM_SKUID,bloomDataQueryService );
    }
}
