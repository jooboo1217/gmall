package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.ValueSkuJsonTo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.atguigu.gmall.product.mapper.SpuSaleAttrMapper;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
* @author 10760
* @description 针对表【spu_sale_attr(spu销售属性)】的数据库操作Service实现
* @createDate 2022-08-23 10:17:45
*/
@Service
public class SpuSaleAttrServiceImpl extends ServiceImpl<SpuSaleAttrMapper, SpuSaleAttr>
    implements SpuSaleAttrService{

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Override
    public List<SpuSaleAttr> getSaleAttrAndValue(Long spuId) {

        //在查询销售属性的同时，需要将值一起查询出来

        List<SpuSaleAttr> list = spuSaleAttrMapper.getAttrAndValue(spuId);

        return list;
    }

    @Override
    public List<SpuSaleAttr> getSaleAttrAndValueMarkSku(Long spuId, Long skuId) {

        return spuSaleAttrMapper.getSaleAttrAndValueMarkSku(spuId,skuId);
    }

    @Override
    public String getAllSkuSaleAttrValueJson(Long spuId) {
        //从数据库查出所有的valueJson组合
        List<ValueSkuJsonTo> valueSkuJsonTos = spuSaleAttrMapper.getAllSkuSaleAttrValueJson(spuId);
        //创建一个map集合，将这些组成存储起来，因为详情也上需要的是json字符串
        HashMap<String, Long> map = new HashMap<>();
        //遍历
        for (ValueSkuJsonTo valueSkuJsonTo : valueSkuJsonTos) {
            String valueJson = valueSkuJsonTo.getValueJson();
            Long skuId = valueSkuJsonTo.getSkuId();
            map.put(valueJson,skuId);
        }
        //将map转换为json字符串
        Gson gson = new Gson();
        String  valueSkuJson = gson.toJson(map);

        return valueSkuJson;
    }
}




