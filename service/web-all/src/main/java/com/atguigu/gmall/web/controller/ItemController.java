package com.atguigu.gmall.web.controller;



import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.item.SkuDetailFeignClient;

import com.atguigu.gmall.model.to.SkuDetailTo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品详情
 */
@Controller
public class ItemController {


    @Autowired
    SkuDetailFeignClient skuDetailFeignClient;
    /**
     * 商品详情页
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String item(@PathVariable("skuId") Long skuId,
                       Model model){

        //远程查询出商品的详细信息
        Result<SkuDetailTo> result = skuDetailFeignClient.getSkuDetail(skuId);




        if(result.isOk()){
            SkuDetailTo skuDetailTo = result.getData();

            //对查询过来的数据进行判断，有可能查到的信息为null
            if (skuDetailTo == null || skuDetailTo.getSkuInfo() == null){
                return "index/404";
            }

            model.addAttribute("categoryView",skuDetailTo.getCategoryView());
            model.addAttribute("skuInfo",skuDetailTo.getSkuInfo());
            model.addAttribute("price",skuDetailTo.getPrice());
            model.addAttribute("spuSaleAttrList",skuDetailTo.getSpuSaleAttrList());//spu的销售属性列表
            model.addAttribute("valuesSkuJson",skuDetailTo.getValuesSkuJson());//json
        }
        return "item/index";
    }
}