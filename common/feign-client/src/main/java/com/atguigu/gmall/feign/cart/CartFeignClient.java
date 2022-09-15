package com.atguigu.gmall.feign.cart;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@RequestMapping("/api/inner/rpc/cart")
@FeignClient("service-cart")
public interface CartFeignClient {

    @GetMapping("/addCart")
    Result<SkuInfo> addToCart(@RequestParam("skuId")Long skuId,
                              @RequestParam("skuNum") Integer skuNum );


    /**
     * 删除勾选中的所有商品
     * @return
     */
    @GetMapping("/deleteChecked")
    Result deleteChecked();


    /**
     * 获取所有选中的商品
     * @return
     */
    @GetMapping("/checked/list")
    Result<List<CartInfo>> getChecked();
}
