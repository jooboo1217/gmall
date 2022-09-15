package com.atguigu.gmall.order;

import com.atguigu.gmall.annotation.EnableAppRabbit;
import com.atguigu.gmall.common.config.annotation.EnableAutoExceptionHandler;
import com.atguigu.gmall.common.config.annotation.EnableAutoFeignInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;



@EnableAppRabbit
@EnableTransactionManagement
@EnableAutoExceptionHandler
@EnableAutoFeignInterceptor //开启feign 用户id透传拦截器
@MapperScan("com.atguigu.gmall.order.mapper")
@SpringCloudApplication
@EnableFeignClients(value = {"com.atguigu.gmall.feign.cart"
                     ,"com.atguigu.gmall.feign.product"
                    ,"com.atguigu.gmall.feign.ware"
                    ,"com.atguigu.gmall.feign.user"
})
public class OrderMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderMainApplication.class,args);
    }
}
