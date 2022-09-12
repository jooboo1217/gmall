package com.atguigu.gmall.common.auth;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class AuthUtils {
    /**
     * 利用Tomcat请求与线程绑定机制。+ Spring自己的 RequestContextHolder ThreadLocal原理
     *      = 同一个请求在处理期间，任何时候都能共享到数据
     * @return
     */
    
    public static UserAuthInfo getCurrentAuthInfo(){
        
        //1线程中获得请求数据
        ServletRequestAttributes requestAttributes 
                = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        //2.获得信息
        UserAuthInfo userAuthInfo = new UserAuthInfo();//userId
        String header = request.getHeader(SysRedisConst.USERID_HEADER);//USERID_HEADER--在请求头中存储的key
        if (!StringUtils.isEmpty(header)){
            userAuthInfo.setUserId(Long.parseLong(header));
        }

        String userTempId = request.getHeader(SysRedisConst.USERTEMPID_HEADER);
        userAuthInfo.setUserTempId(userTempId);//无论临时id是否存在都添加
    
        return userAuthInfo;
    }
}
