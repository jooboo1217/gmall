package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/rpc/cart")
public class CartController {
    
    @Autowired
    CartService cartService;
    
    //添加商品到购物车
    @GetMapping("/addCart")
    public Result<SkuInfo> addCart(@RequestParam("skuId")Long skuId,
                                   @RequestParam("skuNum") Integer skuNum ){

        /**
         * 将商品保存到redis中使用hash存储；
         *      hash：  key cartKey（userId或者userTempId）
         *              hash：  key   skuId
         *                      value  商品的信息  
         */
        //在添加购物车成功后需要显示加入到购物车的信息，信息需要从product中取，需要远程调用product查询加入购物车的信息
        SkuInfo skuInfo = cartService.addToCart(skuId,skuNum);
        
        return Result.ok(skuInfo);
    }
    
 
}
