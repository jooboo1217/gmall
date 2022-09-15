package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * 删除勾选中的所有商品
     * @return
     */
    @GetMapping("/deleteChecked")
    public  Result deleteChecked(){
        //获取是哪个购物车
        String cartKey = cartService.determinCartKey();
        cartService.deleteChecked(cartKey);
        return Result.ok();
    }


    /**
     * 获取所有选中的商品
     * @return
     */
    @GetMapping("/checked/list")
    public Result<List<CartInfo>> getChecked(){
        String cartKey = cartService.determinCartKey();
        List<CartInfo> checkedItems = cartService.getCheckedItems(cartKey);
        return Result.ok(checkedItems);
    }

}
