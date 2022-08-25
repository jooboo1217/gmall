package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.product.SpuSaleAttrValue;
import com.atguigu.gmall.product.service.SpuImageService;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.atguigu.gmall.product.service.SpuSaleAttrValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.product.service.SpuInfoService;
import com.atguigu.gmall.product.mapper.SpuInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 10760
* @description 针对表【spu_info(商品表)】的数据库操作Service实现
* @createDate 2022-08-23 10:17:45
*/
@Service
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoMapper, SpuInfo>
    implements SpuInfoService{

    @Autowired
    SpuImageService spuImageService;

    @Autowired
    SpuSaleAttrService spuSaleAttrService;

    @Autowired
    SpuSaleAttrValueService spuSaleAttrValueService;

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        /**
         * 涉及四张表的更新，一张一张进行更新
         */
        //1.先将spuinfo中的信息更新
        save(spuInfo);
        //获得添加添加进去的spu的id
        Long spuId = spuInfo.getId();

        //2.保存照片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        //遍历图片，将每张图片的spuId附上
        for (SpuImage spuImage : spuImageList) {
            spuImage.setSpuId(spuId);
        }
        //批量保存照片
        spuImageService.saveBatch(spuImageList);

        //3.保存spu的销售属性
        //销售属性中包含了销售值
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        //遍历给每个
        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            spuSaleAttr.setSpuId(spuId);
            //获得每一个spu属性对应的属性值
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            //遍历，将spuid给附上
            for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                spuSaleAttrValue.setSpuId(spuId);
                //将属性名给值附上
                spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
            }
            //批量保存spu的属性值
            spuSaleAttrValueService.saveBatch(spuSaleAttrValueList);
        }
        //批量保存spu的属性
        spuSaleAttrService.saveBatch(spuSaleAttrList);
    }
}




