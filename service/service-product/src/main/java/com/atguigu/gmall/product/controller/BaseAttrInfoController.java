package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.product.service.BaseAttrInfoService;
import com.atguigu.gmall.product.service.BaseAttrValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class BaseAttrInfoController {
    @Autowired
    BaseAttrInfoService baseAttrInfoService;
    @Autowired
    BaseAttrValueService baseAttrValueService;
    //展示没级分类下的属性
    ///attrInfoList/1/0/0
    @GetMapping("/attrInfoList/{c1Id}/{c2Id}/{c3Id}")
    public Result getAttrInfoList(@PathVariable("c1Id")Long c1Id,
                                  @PathVariable("c2Id")Long c2Id,
                                  @PathVariable("c3Id")Long c3Id){
        List<BaseAttrInfo> list =baseAttrInfoService.getAttrInfoListById(c1Id,c2Id,c3Id);

        return Result.ok(list);
    }
    //添加保存属性
    //http://192.168.6.1/admin/product/saveAttrInfo
    //要添加的数据在请求体中
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo ){

        baseAttrInfoService.saveAttrInfoAndValue(baseAttrInfo);

        return Result.ok();
    }

    ///admin/product/getAttrValueList/11
    //点击修改值
    @GetMapping("/getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable("attrId")Long attrId){

        List<BaseAttrValue>values = baseAttrValueService.getAttrValueList(attrId);

        return Result.ok(values);
    }

}
