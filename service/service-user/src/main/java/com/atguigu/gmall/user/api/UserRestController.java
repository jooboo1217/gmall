package com.atguigu.gmall.user.api;

import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inner/rpc/user")
public class UserRestController {
    
    @Autowired
    UserAddressService userAddressService;

    /**
     * 查询用户的所有地址
     * @return
     */
    @GetMapping("/address/list")
    public Result<List<UserAddress>> getUserAddr(){
        //获得当前登录的用户
        UserAuthInfo userAuthInfo = AuthUtils.getCurrentAuthInfo();
        //获得当前登录用户的userId
        Long userId = userAuthInfo.getUserId();
        //查询该用户对应的地址
        List<UserAddress> list = userAddressService
                .list(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId));
        
        return Result.ok(list);
    }
}
