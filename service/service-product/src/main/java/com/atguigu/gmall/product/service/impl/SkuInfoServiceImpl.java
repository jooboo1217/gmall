package com.atguigu.gmall.product.service.impl;


import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import com.atguigu.gmall.product.mapper.BaseCategory3Mapper;
import com.atguigu.gmall.product.service.*;
import com.atguigu.stater.cache.constant.SysRedisConst;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.product.mapper.SkuInfoMapper;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
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

    @Resource
    SkuInfoMapper skuInfoMapper;

    @Resource
    BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    SpuSaleAttrService spuSaleAttrService;


    @Autowired
    RedissonClient redissonClient;

    @Transactional
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

        /**
         * 在新增商品的时候，直接将商品的id存储到布隆过滤器中
         */

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(SysRedisConst.BLOOM_SKUID);
        bloomFilter.add(skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        skuInfoMapper.updateIssale(skuId,0);
    }

    @Override
    public void onSale(Long skuId) {
        skuInfoMapper.updateIssale(skuId,1);
    }

    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {

        /**
         *  * 分析根据详情页面的内容，分析需要的内容
         *      * 1.该spu对应的三级分类
         *      * 2.该sku对应的图片
         *      * 3.该sku对应信息skuInfo
         *      * 4.价格是动态获取的，需要单拿
         *      * 5.该spu的销售属性,该sku对应的需要高亮显示
         */
        SkuDetailTo detailTo = new SkuDetailTo();
        //(√) 0、查询到skuInfo
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);


        //(√) 2、商品（sku）的基本信息【价格、重量、名字...】   sku_info
        //把查询到的数据一定放到 SkuDetailTo 中
        detailTo.setSkuInfo(skuInfo);

        //(√) 3、商品（sku）的图片        sku_image
        List<SkuImage> imageList = skuImageService.getSkuImage(skuId);
        skuInfo.setSkuImageList(imageList);


        //(√) 1、商品（sku）所属的完整分类信息：  base_category1、base_category2、base_category3
        CategoryViewTo categoryViewTo = baseCategory3Mapper.getCategoryView(skuInfo.getCategory3Id());
        detailTo.setCategoryView(categoryViewTo);

        //(√) 实时价格查询
        BigDecimal price = get1010Price(skuId);
        detailTo.setPrice(price);





        //(√)4、商品（sku）所属的SPU当时定义的所有销售属性名值组合（固定好顺序）。
        //          spu_sale_attr、spu_sale_attr_value
        //          并标识出当前sku到底spu的那种组合，页面要有高亮框 sku_sale_attr_value
        //查询当前sku对应的spu定义的所有销售属性名和值（固定好顺序）并且标记好当前sku属于哪一种组合
        List<SpuSaleAttr> saleAttrList = spuSaleAttrService
                .getSaleAttrAndValueMarkSku(skuInfo.getSpuId(),skuId);
        detailTo.setSpuSaleAttrList(saleAttrList);

        //5、商品（sku）类似推荐    （x）
        //valuesSkuJson
        Long spuId = skuInfo.getSpuId();
        String valuesSkuJson =  spuSaleAttrService.getAllSkuSaleAttrValueJson(spuId);
        detailTo.setValuesSkuJson(valuesSkuJson);

        //6、商品（sku）介绍[所属的spu的海报]        spu_poster（x）
        //7、商品（sku）的规格参数                  sku_attr_value
        //8、商品（sku）售后、评论...              相关的表 (x)


        return detailTo;
    }


    @Override
    public BigDecimal get1010Price(Long skuId) {
        //性能低下
        BigDecimal price = skuInfoMapper.getRealPrice(skuId);
        return price;
    }

    //通过skuId查询sku的所有信息
    @Override
    public SkuInfo getDetailSkuInfo(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        return skuInfo;
    }
    //通过skuId查询所有对应的图片
    @Override
    public List<SkuImage> getDetailSkuImages(Long skuId) {
        List<SkuImage> skuImage = skuImageService.getSkuImage(skuId);
        return skuImage;
    }

    @Override
    public List<Long> findAllSkuId() {
       List<Long> skuIds =  skuInfoMapper.getAllSkuId();
        return skuIds;
    }
}




