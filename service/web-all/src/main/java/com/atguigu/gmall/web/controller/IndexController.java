package com.atguigu.gmall.web.controller;



import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.product.CategoryTreeFeign;
import com.atguigu.gmall.model.to.CategoryTreeTo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    CategoryTreeFeign categoryTreeFeign;
    //访问欢迎页
    @GetMapping({"/","/index"})
    public String indexPage(Model model){

        //分析：首页上需要显示三级，三家分类从产品微服务可以得到，需要使用远程调用
        //通过对index页面的分析，三级分类的数据包括
        /**
         * 一级分类:category1.categoryId   category1.categoryName
         * 二级分类:category2.categoryId   category2.categoryName
         * 三级分类:category3.categoryId   category3.categoryName
         */
        //创建一个bean存储查询到的数据
        Result<List<CategoryTreeTo>> allCategoryWithTree = categoryTreeFeign.getAllCategoryWithTree();

        if (allCategoryWithTree.isOk()){
            List<CategoryTreeTo> data = allCategoryWithTree.getData();
            //将数据存储到Model中
            model.addAttribute("list",data);
        }
        return "index/index";
    }
}
