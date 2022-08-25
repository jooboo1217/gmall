package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.SkuAttrValue;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SkuSaleAttrValue;
import com.atguigu.gmall.product.service.SkuAttrValueService;
import com.atguigu.gmall.product.service.SkuImageService;
import com.atguigu.gmall.product.service.SkuSaleAttrValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.product.service.SkuInfoService;
import com.atguigu.gmall.product.mapper.SkuInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 10760
* @description 针对表【sku_info(库存单元表)】的数据库操作Service实现
* @createDate 2022-08-23 10:17:45
*/
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo>
    implements SkuInfoService{

    @Autowired
    SkuImageService skuImageService;
    @Autowired
    SkuAttrValueService skuAttrValueService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    SkuInfoMapper skuInfoMapper;


    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //分析，需要保存到哪些表中
        /**
         * sku_info
         * sku_image
         * sku_attr_value
         * sku_sale_attr_value
         */
        //1.先保存基本数据
        save(skuInfo);
        //获得sku的id
        Long skuId = skuInfo.getId();
        //2.保存图片到skuImg
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        //遍历
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
        }
        //存储
        skuImageService.saveBatch(skuImageList);
        //3.存储平台属性名和属性值
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
        }
        skuAttrValueService.saveBatch(skuAttrValueList);

        //4.存储销售属性名和销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
        }
        skuSaleAttrValueService.saveBatch(skuSaleAttrValueList);
    }

    @Override
    public void cancelSale(Long skuId) {
        skuInfoMapper.updateIssale(skuId,0);
    }

    @Override
    public void onSale(Long skuId) {
        skuInfoMapper.updateIssale(skuId,1);
    }


}




