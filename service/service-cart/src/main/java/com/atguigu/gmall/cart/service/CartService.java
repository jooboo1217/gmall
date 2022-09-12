package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;

import java.util.List;

public interface CartService {
    /**
     * 添加商品到购物车
     * @param skuId
     * @param skuNum
     * @return
     */
    SkuInfo addToCart(Long skuId, Integer skuNum);

    /**
     * 往购物车中添加数据
     * @param skuId
     * @param skuNum
     * @param
     * @return
     */
    
    SkuInfo addItemToCart(Long skuId, Integer skuNum, String cartKey);
    /**
     * 将从数据库中查询到的skuInfo数据转换为子在redis中存储的数据类型
     * @param skuInfo
     * @return
     */
    
    CartInfo converSkuInfo2CartInfo(SkuInfo skuInfo);
    /**
     * 因为使用的是同一个线程。可以通过localthread查到请求头中的id（userId  userTempId）
     * @return
     */
    String determinCartKey();

    /**
     * 根据购物车键和商品的id查询商品的信息
     * @param cartKey
     * @param skuId
     * @return
     */
     CartInfo getItemFromCart(String cartKey, Long skuId);

    /**
     * 将cartInfo转换为skuInfo
     * @param cartInfo
     * @return
     */
    SkuInfo converCartInfo2SkuInfo(CartInfo cartInfo);

    /**
     * 合并临时购物车中的商品到用户的购物车
     */
    void mergeUserAndTempCart();

    /**
     * 获取购物车所有商品
     * @param cartKey
     * @return
     */
    List<CartInfo> getCartList(String cartKey);

    /**
     * 更新购物车中商品的数量
     * @param skuId
     * @param num
     * @param cartKey
     */
    void updateItemNum(Long skuId, Integer num, String cartKey);

    /**
     * 删除购物车中的一个品类
     * @param skuId
     */
    void deleteCart(Long skuId,String cartKey);

    /**
     * 修改凑无车页面的选中状态
     * @param skuId
     * @param cartKey
     */
    void updateChecked(Long skuId, String cartKey,Integer status);
    
}
