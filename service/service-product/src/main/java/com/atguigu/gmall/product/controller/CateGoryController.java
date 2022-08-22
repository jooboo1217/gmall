package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.product.service.BaseCategory1Service;
import com.atguigu.gmall.product.service.BaseCategory2Service;
import com.atguigu.gmall.product.service.BaseCategory3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class CateGoryController {
    @Autowired
    BaseCategory1Service baseCategory1Service;
    @Autowired
    BaseCategory2Service baseCategory2Service;
    @Autowired
    BaseCategory3Service baseCategory3Service;
    //查询一级分类
    @GetMapping("/getCategory1")
    public Result getCateGory1(){
        List<BaseCategory1> list = baseCategory1Service.list();
        return Result.ok(list);
    }
    //根据一级分类，查找二级分类
    @GetMapping("/getCategory2/{c1Id}")
    public Result getCateGory2(@PathVariable("c1Id")Long c1Id){
           List<BaseCategory2> list = baseCategory2Service.getCateGory2List(c1Id);
           return Result.ok(list);
    }
    //根据二级分类，查找三级分类
    @GetMapping("//getCategory3/{c2Id}")
    public Result getCateGory3(@PathVariable("c2Id")Long c2Id){
        List<BaseCategory3> list =   baseCategory3Service.getCateGory2List(c2Id);
        return Result.ok(list);
    }

}
