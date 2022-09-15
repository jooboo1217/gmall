package com.atguigu.gmall.order.biz;

import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import com.atguigu.gmall.model.vo.order.OrderSubmitVo;

public interface OrderBizService {
    /**
     * 获得订单确认页的数据
     * @return
     */
    OrderConfirmDataVo getOrderConfirmData();

    /**
     * 创建令牌
     * @return
     */
    String generateTradeNo();

    /**
     * 提交订单的方法
     * @param submitVo
     * @param tradeNo
     * @return
     */
    Long submitOrder(OrderSubmitVo submitVo, String tradeNo);

    /**
     * 关闭订单
     * @param orderId
     * @param userId
     */
    void closeOrder(Long orderId, Long userId);
}
