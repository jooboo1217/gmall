package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.SkuInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SkuController {
    @Autowired
    SkuInfoService skuInfoService;
    //http://192.168.6.1/admin/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        //保存sku的信息，需要保存到四个表中
        skuInfoService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    //sku管理页面显示所有的sku
    //http://192.168.6.1/admin/product/list/1/10
    @GetMapping("/list/{pageSize}/{pageNum}")
    public Result list(@PathVariable("pageSize")Integer pageSize,
                       @PathVariable("pageNum")Integer pageNum){
        Page<SkuInfo> page = new Page<>(pageSize,pageNum);

        Page<SkuInfo> infoPage = skuInfoService.page(page);

        return Result.ok(infoPage);
    }
    //sku页面的下架按钮
    //http://192.168.6.1/admin/product/cancelSale/40
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable("skuId")Long skuId){
        skuInfoService.cancelSale(skuId);
        return Result.ok();
    }
    //sku页面的上架按钮
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable("skuId")Long skuId){
        //http://192.168.6.1/admin/product/onSale/48

        skuInfoService.onSale(skuId);

        return Result.ok();
    }
}
