package com.atguigu.gmall.order.service;


import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import com.atguigu.gmall.model.vo.order.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 10760
* @description 针对表【order_info(订单表 订单表)】的数据库操作Service
* @createDate 2022-09-13 20:30:56
*/
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 保存订单到数据库
     * @param submitVo
     * @param tradeNo
     * @return
     */
    Long saveOrder(OrderSubmitVo submitVo, String tradeNo);

    /**
     * 改变订单的状态
     * @param orderId
     * @param userId
     * @param closed
     * @param expected
     */
    void changeOrderStatus(Long orderId,
                           Long userId, 
                           ProcessStatus closed, 
                           List<ProcessStatus> expected);
}
