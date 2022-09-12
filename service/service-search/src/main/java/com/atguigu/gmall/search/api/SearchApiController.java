package com.atguigu.gmall.search.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.vo.search.SearchParamVo;
import com.atguigu.gmall.model.vo.search.SearchResponseVo;
import com.atguigu.gmall.search.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/inner/rpc/search")
public class SearchApiController {
    
    @Autowired
    GoodsService goodsService;

    /**
     *保存到es 
     * @return
     */
    @RequestMapping("/goods")
    public Result saveGoods(@RequestBody Goods goods){
        goodsService.saveGoods(goods);
        return Result.ok();
    }
    
    @DeleteMapping("/goods/{skuId}")
    public Result deleteGoods(@PathVariable("skuId")Long skuId){
        goodsService.deleteGoods(skuId);
        return Result.ok();
    }

    /**
     * 通过检索页传送回过来的检索属性，到es中查找数据
     * @param searchParamVo
     * @return
     */
    @PostMapping("/goods/search")
    Result<SearchResponseVo> search(@RequestBody SearchParamVo searchParamVo){
        SearchResponseVo responseVo = goodsService.search(searchParamVo);
        return Result.ok(responseVo);
    }

    /**
     * 热点人数，访问的数越多，分数越高，在排序的总和项使用
     */
    /**
     * 更新热度分
     * @param skuId
     * @param score 商品最新的得分
     * @return
     */
    @GetMapping("/goods/hotscore/{skuId}")
    public Result updateHotScore(@PathVariable("skuId") Long skuId,
                                 @RequestParam("score") Long score,
                                 HttpServletResponse response){
        goodsService.updateHotScore(skuId,score);

        /**
         * 会话Cookie；
         * 1)、默认当前会话有效。只要浏览器关闭就销毁
         * 2)、每个 Cookie 都有自己的作用域范围。
         */
//        Cookie cookie = new Cookie("token","uuidjjuydjafhajkdsfh");
//        cookie.setMaxAge(60000000);
//        cookie.setDomain(".jd.com"); //访问 jd.com以及任意子域名都带
//        response.addCookie(cookie);
        return Result.ok();
    }

}
