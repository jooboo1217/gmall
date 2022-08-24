package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product")
public class TradeMarkController {
    //http://192.168.6.1/admin/product/baseTrademark/1/10
    @Autowired
    BaseTrademarkService baseTrademarkService;

    //分页查询品牌
    @GetMapping("/baseTrademark/{pageSize}/{pageNum}")
    public Result getBaseTrademark(@PathVariable("pageSize")Integer pageSize,
                                   @PathVariable("pageNum")Integer pageNum){

        //分页查询品牌
        Page<BaseTrademark> page = new Page<>(pageSize,pageNum);

        Page<BaseTrademark> result = baseTrademarkService.page(page);
        return Result.ok(result);
    }

    //删除品牌
    @DeleteMapping("/baseTrademark/remove/{delId}")
    public Result delTradeMark(@PathVariable("delId")Long delId ){
        //http://192.168.6.1/admin/product/baseTrademark/remove/2
        baseTrademarkService.removeById(delId);
        return  Result.ok();
    }
    //增加品牌
    //http://192.168.6.1/admin/product/baseTrademark/save
    @PostMapping("/baseTrademark/save")
    public Result saveTradeMark(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //修改品牌--查询品牌
    //http://192.168.6.1/admin/product/baseTrademark/get/14
    @GetMapping("/baseTrademark/get/{tmId}")
    public Result getTradeMark(@PathVariable("tmId")Long tmId){
        BaseTrademark trademark = baseTrademarkService.getById(tmId);
        return Result.ok(trademark);
    }
    //修改品牌--保存品牌
    //http://192.168.6.1/admin/product/baseTrademark/update
    @PutMapping("/baseTrademark/update")
    public Result updateTradeMark(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }
}
