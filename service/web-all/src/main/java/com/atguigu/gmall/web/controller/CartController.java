package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.cart.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {
    
    @Autowired
    CartFeignClient cartFeignClient;
    
    //加入购物车成功后的页面
    @GetMapping("/addCart.html")
    public String toCart(@RequestParam("skuId")Long skuId,
                         @RequestParam("skuNum") Integer skuNum,
                         Model model){

        Result<SkuInfo> result = cartFeignClient.addToCart(skuId, skuNum);
        if (result.isOk()){
            model.addAttribute("skuInfo",result.getData());
            model.addAttribute("skuNum",skuNum);
            return "cart/addCart";
        }else {
            String message = result.getMessage();
            model.addAttribute("msg",result.getData());
            return "cart/error";
        }
    }
    
    //购物车页面和去购物车结算页面
    @GetMapping("/cart.html")
    public String cartHtml(){
        //显示购物车信息是一个异步请求，不需要远程调用
        return "cart/index";
    }
}
