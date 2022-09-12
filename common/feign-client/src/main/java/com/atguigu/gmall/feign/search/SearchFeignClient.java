package com.atguigu.gmall.feign.search;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.vo.search.SearchParamVo;
import com.atguigu.gmall.model.vo.search.SearchResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/inner/rpc/search")
@FeignClient("service-search")
public interface SearchFeignClient {

    @RequestMapping("/goods")
    public Result saveGoods(@RequestBody Goods goods);

    @DeleteMapping("/goods/{skuId}")
    public Result deleteGoods(@PathVariable("skuId")Long skuId);
    
    @PostMapping("/goods/search")
    Result<SearchResponseVo> search(SearchParamVo searchParamVo);

    @GetMapping("/goods/hotscore/{skuId}")
    Result updateHotScore(@PathVariable("skuId") Long skuId,
                          @RequestParam("score") Long score);

    /**
     * 热点人数，访问的数越多，分数越高，在排序的总和项使用
     */
  
}
