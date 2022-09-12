package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartRestController {
    
    @Autowired
    CartService cartService;

    /**
     * 购物车列表--获得购物车中的所有商品
     * @return
     */
    @GetMapping("/cartList")
    public Result cartList(){
        //1.决定用哪个购物车键
        String cartKey = cartService.determinCartKey();
        //2.尝试合并购物车
        cartService.mergeUserAndTempCart();
        //3.收集购物车的信息
        List<CartInfo> infos = cartService.getCartList(cartKey);
        
        return Result.ok(infos);
    }

    /**
     * 修改购物车中的某个商品的数量
     * @param skuId
     * @param num
     * @return
     */
    @PostMapping("/addToCart/{skuId}/{num}")
    public Result addToCart(@PathVariable("skuId")Long skuId,
                            @PathVariable("num")Integer num){
        String cartKey = cartService.determinCartKey();
        cartService.updateItemNum(skuId,num,cartKey);

        return Result.ok();
    }

    /**
     * 删除购物车中的一个品类
     * @param skuId
     * @return
     */
    @RequestMapping("/deleteCart/{skuId}")
    public Result deleteCart(@PathVariable("skuId")Long skuId){
        //决定是哪个购物车
        String cartKey = cartService.determinCartKey();
        //删除购物车的一个品类
        cartService.deleteCart(skuId,cartKey);
        return Result.ok();
    }
    
    @GetMapping("/checkCart/{skuId}/{status}")
    public Result checkCart(@PathVariable("skuId")Long skuId,
                            @PathVariable("status")Integer status){
        //决定是哪个购物车
        String cartKey = cartService.determinCartKey();
        //修改购物车的勾选项
        cartService.updateChecked(skuId,cartKey,status);

        return Result.ok();
    }
}
