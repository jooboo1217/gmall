package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.order.service.CartInfoService;
import com.atguigu.gmall.order.mapper.CartInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author 10760
* @description 针对表【cart_info(购物车表 用户登录系统时更新冗余)】的数据库操作Service实现
* @createDate 2022-09-11 18:27:20
*/
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo>
    implements CartInfoService{

}




