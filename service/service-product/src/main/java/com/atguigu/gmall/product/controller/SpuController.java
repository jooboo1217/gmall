package com.atguigu.gmall.product.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.SpuImageService;
import com.atguigu.gmall.product.service.SpuInfoService;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SpuController {


    @Autowired
    SpuInfoService spuInfoService;

    @Autowired
    SpuSaleAttrService spuSaleAttrService;

    @Autowired
    SpuImageService spuImageService;
    //分页查询spu的信息
    //http://192.168.6.1/admin/product/1/10?category3Id=61
    @GetMapping("/{pageSize}/{pageNum}")
    public Result getSpuPage(@PathVariable("pageSize")Long pageSize,
                             @PathVariable("pageNum")Long pageNum,
                             @RequestParam("category3Id")Long category3Id){

        Page<SpuInfo> page = new Page<>(pageSize, pageNum);

        //条件
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);

        Page<SpuInfo> result = spuInfoService.page(page, queryWrapper);
        return Result.ok(result);
    }


    //保存新增的spu
    /**
     * 通过对spu新增页面的分析，共涉及四张表需要进行添加
     * 1.SpuInfo
     * 2.SpuImage
     * 3.SpuSaleAttr
     * 4.SpuSaleAttrValue
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpu(@RequestBody SpuInfo spuInfo){
        //http://192.168.6.1/admin/product/saveSpuInfo
        spuInfoService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    //点击了添加sku，需要将spu的所有的属性和所有的照片回显
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable("spuId")Long spuId){
        //http://192.168.6.1/admin/product/spuSaleAttrList/24
        List<SpuSaleAttr> list =spuSaleAttrService.getSaleAttrAndValue(spuId);

        return Result.ok(list);
    }
    //照片回显到添加sku的页面
    @GetMapping("/spuImageList/{spuId}")
    public Result spuImageList(@PathVariable("spuId")Long spuId){
        //http://192.168.6.1/admin/product/spuImageList/24
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id",spuId);
        List<SpuImage> list = spuImageService.list(queryWrapper);
        return Result.ok(list);
    }

}
