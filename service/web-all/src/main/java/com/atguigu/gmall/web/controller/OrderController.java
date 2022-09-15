package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.order.OrderFeignClient;
import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderController {
    @Autowired
    OrderFeignClient orderFeignClient;
    
    @GetMapping("/trade.html")
    public String tradePage(Model model){
        Result<OrderConfirmDataVo> data = orderFeignClient.getOrderConfirmData();
        
        if (data.isOk()) {
            OrderConfirmDataVo orderInfo = data.getData();
            model.addAttribute("detailArrayList",orderInfo.getDetailArrayList());
            model.addAttribute("totalNum",orderInfo.getTotalNum());
            model.addAttribute("totalAmount",orderInfo.getTotalAmount());
            model.addAttribute("userAddressList",orderInfo.getUserAddressList());
            model.addAttribute("tradeNo",orderInfo.getTradeNo());
        }
        return "order/trade";
    }
}
