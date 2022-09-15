package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.feign.product.SkuProductFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import com.atguigu.stater.cache.utils.Jsons;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServcieImpl implements CartService {
    
    @Autowired
    StringRedisTemplate redisTemplate;
    
    @Autowired
    SkuProductFeignClient productFeignClient;
    
    @Autowired
    ThreadPoolExecutor executor;
    
    @Autowired
    SkuProductFeignClient skuFeignClient;
    
    /**
     * 远程调用上平服务查询加入购物车的商品信息
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public SkuInfo addToCart(Long skuId, Integer skuNum) {
        //在存储的时候要判断是用的临时的账户在存储还是用的用户登录的账号存储
        //1.判断决定购物车使用哪个键
        String cartKey = determinCartKey();
        //2.给购物车添加指定的商品
        SkuInfo skuInfo = addItemToCart(skuId,skuNum,cartKey);
        //3.购物车超时设置，自动延期
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        if(authInfo.getUserId() == null){
            //用户未登录状态一直操作临时购物车
            String tempKey = SysRedisConst.CART_KEY + authInfo.getUserTempId();
            //临时购物车都有过期时间，自动延期
            redisTemplate.expire(tempKey,90, TimeUnit.DAYS);
        }
        
        return skuInfo;
    }

    /**
     * 往购物车中添加数据
     * @param skuId
     * @param skuNum
     * @param cartKey
     * @return
     */
    public SkuInfo addItemToCart(Long skuId, Integer skuNum, String cartKey) {
        /**
         * 分析：
         *  1.该购物车是否已经创建，如果是第一次添加，就先创建购物车然后再添加商品
         *  2.购物车已经创建，直接往购物车中添加
         *        2.1需要考虑该商品是不是已经添加到了该购物车中，如果已经添加了，就只改变商品的数量，修改时间
         */
        //有就拿到这个购物车，没有就创建这个购物车
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        //判断这个购物车中是否存在要存储的这个商品
        Boolean haskey = hashOps.hasKey(skuId.toString());
        //在往一个购物车中添加品类的时候要进行数量的限制，最多只能添加二百个品类
        Long cartSize = hashOps.size();
        if (!haskey){//购物车中不存在这个品类，第一次添加
            //先判断购物车是否装满了
            if (cartSize+1 > SysRedisConst.CART_ITEMS_LIMIT){
               throw  new GmallException(ResultCodeEnum.CART_OVERFLOW);
            }
            //根据skuId查询商品的详情
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId).getData();
            //将skuInfo转换为cartInfo
            CartInfo item = converSkuInfo2CartInfo(skuInfo);
            //设置加入购物车的数量
            item.setSkuNum(skuNum);
            //将转好的商品信息添加到redis中
            hashOps.put(skuId.toString(), Jsons.toStr(item));
            //返回skuInfo
            return skuInfo;
        }else{//不是第一次存储这个商品类
            /**
             * 1.获得实时价格，更新
             * 2.更新修改的时间
             * 3.修改商品的数量
             */
            //先获得产品的信息
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId).getData();
            //获得实时价格
            Result<BigDecimal> sku1010Price = productFeignClient.getSku1010Price(skuId);
            //获得原来的信息
            CartInfo cartInfo = getItemFromCart(cartKey,skuId);
            //对原来的信息进行更新
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            cartInfo.setUpdateTime(new Date());
            cartInfo.setSkuPrice(sku1010Price.getData());
           //同步到redis中进行覆盖
            hashOps.put(skuInfo.toString(),Jsons.toStr(cartInfo));
            //将cartInfo转换为skuInfo返回
            SkuInfo Info = converCartInfo2SkuInfo(cartInfo);
            return Info;
        }
    }

    /**
     * 将cartInfo转换为skuInfo
     * @param cartInfo
     * @return
     */
    public SkuInfo converCartInfo2SkuInfo(CartInfo cartInfo) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSkuName(cartInfo.getSkuName());
        skuInfo.setSkuDefaultImg(cartInfo.getImgUrl());
        skuInfo.setId(cartInfo.getSkuId());
        
        return skuInfo;
    }
    //合并购物车方法
    @Override
    public void mergeUserAndTempCart() {
        //从threaload中获得权限信息
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        //1.判断是否合并需要合并
        if (authInfo.getUserId()!=null && StringUtils.isEmpty(authInfo.getUserTempId())){//判断是否有加入过购物车
            //2.如果临时购物车有商品，合并后并删除购物车
            String tempCartKey = SysRedisConst.CART_KEY + authInfo.getUserTempId();
            //3.获得临时购物车的所有商品
            List<CartInfo> tempCartList = getCartList(tempCartKey);
            if (tempCartList != null && tempCartList.size() > 0){
                //临时购物车有数据，需要合并
                //先获得用户的购物车
                String userCartKey = SysRedisConst.CART_KEY + authInfo.getUserId();
                for (CartInfo info : tempCartList) {
                    Long skuId = info.getSkuId();
                    Integer skuNum = info.getSkuNum();
                    addItemToCart(skuId,skuNum,userCartKey);
                    //合并成一个商品就删除一个,因为购物车的商品加入数是有限制的，200个，如果超了后面的就不加了，也不删
                    redisTemplate.opsForHash().delete(tempCartKey,skuId.toString());
                }
            }
        }
    }    
    
    //收集购物车的商品
    @Override
    public List<CartInfo> getCartList(String cartKey) {
        //获取存在hash中的数据
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(cartKey);
        //流式编程
        List<CartInfo> infos = hashOps.values().stream()
                .map(str -> Jsons.toObj(str, CartInfo.class))
                .sorted((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()))
                .collect(Collectors.toList());

        //顺便把购物车中所有商品的价格再次查询一遍进行更新。 异步不保证立即执行。
        //不用等价格更新。 异步情况下拿不到老请求
        //1、老请求
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        //【异步会导致feign丢失请求】
        executor.submit( () ->{
            //2、绑定请求到到这个线程
            RequestContextHolder.setRequestAttributes(attributes);
            updateCartAllItemsPrice(cartKey);
            //3.移除数据
            RequestContextHolder.resetRequestAttributes();
        });

        return infos;
    }

    /**
     * 更新购物车中商品的数量
     * @param skuId
     * @param num
     * @param cartKey
     */
    @Override
    public void updateItemNum(Long skuId, Integer num, String cartKey) {
        //获取购物车
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(cartKey);
            
        //拿到商品
        CartInfo item = getItemFromCart(cartKey, skuId);
        item.setSkuNum(item.getSkuNum()+num);
        item.setUpdateTime(new Date());
        
        //从购物车中修改
        hashOps.put(skuId.toString(),Jsons.toStr(item));
    }


    /**
     * 删除购物车中的一个品类
     * @param skuId
     */
    @Override
    public void deleteCart(Long skuId,String cartKey) {
        //绑定购物车
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(cartKey);
        
        //通过商品id获取删除商品
        hashOps.delete(skuId.toString());
        
    }

    /**
     * 修改凑无车页面的选中状态
     * @param skuId
     * @param cartKey
     */
    @Override
    public void updateChecked(Long skuId, String cartKey,Integer status) {
        //获取购物车
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(cartKey);
        //拿到要修改的商品
        CartInfo item = getItemFromCart(cartKey, skuId);
        item.setIsChecked(status);
        item.setUpdateTime(new Date());
        //保存
        hashOps.put(skuId.toString(),Jsons.toStr(item));
    }

    /**
     * 删除勾选中的商品
     * @param cartKey
     */
    @Override
    public void deleteChecked(String cartKey) {
        //获取购物车
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(cartKey);
        //获取购物车中勾中的商品
        List<String> ids = getCheckedItems(cartKey).stream()
                .map(cartInfo -> cartInfo.getSkuId().toString())
                .collect(Collectors.toList());
        
        if (ids != null && ids.size() > 0 ){
            hashOps.delete(ids.toArray());
        }
    }

    /**
     * 获取所有勾中的商品
     * @param cartKey
     * @return
     */
    @Override
    public List<CartInfo> getCheckedItems(String cartKey) {
        List<CartInfo> cartList = getCartList(cartKey);
        List<CartInfo> checkedItems = cartList.stream()
                .filter(cart -> cart.getIsChecked() == 1)
                .collect(Collectors.toList());
        return checkedItems;
    }

    /**
     * //更新购物车内的价格
     * @param cartKey
     */
    public void updateCartAllItemsPrice(String cartKey) {
        BoundHashOperations<String, String, String> cartOps =
                redisTemplate.boundHashOps(cartKey);

        System.out.println("更新价格启动：" + Thread.currentThread());
        cartOps
                .values()
                .stream()
                .map(str ->
                        Jsons.toObj(str, CartInfo.class)
                ).forEach(cartInfo -> {
                    //1、查出最新价格  15ms
                    Result<BigDecimal> price = skuFeignClient.getSku1010Price(cartInfo.getSkuId());
                    //2、设置新价格
                    cartInfo.setSkuPrice(price.getData());
                    cartInfo.setUpdateTime(new Date());
                    //3、更新购物车价格  5ms。给购物车存数据之前再做一个校验。
                    //100%防得住
                    if(cartOps.hasKey(cartInfo.getSkuId().toString())){
                        cartOps.put(cartInfo.getSkuId().toString(), Jsons.toStr(cartInfo));
                    }

                });

        System.out.println("更新价格结束：" + Thread.currentThread());
    }

    /**
     * 根据购物车键和商品的id查询商品的信息
     * @param cartKey
     * @param skuId
     * @return
     */
    public CartInfo getItemFromCart(String cartKey, Long skuId) {
        //查出购物车
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(cartKey);
        //根据商品id查出商品的信息
        String jsonData = hashOps.get(skuId.toString());
        //转换为对象
        CartInfo cartInfo = Jsons.toObj(jsonData, CartInfo.class);
        return cartInfo;
    }

    /**
     * 将从数据库中查询到的skuInfo数据转换为子在redis中存储的数据类型
     * @param skuInfo
     * @return
     */
    public CartInfo converSkuInfo2CartInfo(SkuInfo skuInfo) {
        //创建cartInfo用来装从skuInfo
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuInfo.getId());
        cartInfo.setCartPrice(skuInfo.getPrice());
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setIsChecked(1);
        cartInfo.setCreateTime(new Date());
        cartInfo.setUpdateTime(new Date());
        cartInfo.setSkuPrice(skuInfo.getPrice());
        
        return cartInfo;
    }

    /**
     * 因为使用的是同一个线程。可以通过localthread查到请求头中的id（userId  userTempId）
     * @return
     */
    public String determinCartKey() {
        //创建一个工具类，获取userId
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        String cartKey = SysRedisConst.CART_KEY;
        //判断用户id是否存在，优先存储用户的id
        if (authInfo.getUserId() != null){
            //用户登录了，就用用户userId做购物车的键
            cartKey = cartKey+""+authInfo.getUserId();  
        }else {
            //用户未登录就用userTempId做购物车键
            cartKey = cartKey+""+authInfo.getUserTempId();
        }
        return cartKey;
    }
}
