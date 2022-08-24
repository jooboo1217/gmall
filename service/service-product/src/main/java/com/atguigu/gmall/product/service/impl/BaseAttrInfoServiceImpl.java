package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.product.mapper.BaseAttrValueMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.atguigu.gmall.product.service.BaseAttrInfoService;
import com.atguigu.gmall.product.mapper.BaseAttrInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author 10760
* @description 针对表【base_attr_info(属性表)】的数据库操作Service实现
* @createDate 2022-08-23 10:17:45
*/
@Service
public class BaseAttrInfoServiceImpl extends ServiceImpl<BaseAttrInfoMapper, BaseAttrInfo>
    implements BaseAttrInfoService{

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;


    @Override
    public List<BaseAttrInfo> getAttrInfoListById(Long c1Id, Long c2Id, Long c3Id) {
        //根据一级分类，二级分类，三级分类的id查找属性名和属性值
        //分析：因为查找的数据需要封装到BaseAttrInfo中，包含属性名也包含属性值所以需要自定义sql从数据库查找然后封装
        List<BaseAttrInfo> list = baseAttrInfoMapper.getAttrInfoAndVlue(c1Id,c2Id,c3Id);

        return list;
    }


    /**
     * 修改后的保存和添加保存共用了一个接口  所以需要进行区分
     * @param baseAttrInfo
     */
    @Override
    public void saveAttrInfoAndValue(BaseAttrInfo baseAttrInfo) {

        /**
         * 1.如果有id则认为是修改
         * 2.没id是新增
         * 3.数据库有，但是本次提交没有，是删除
         */
        if (baseAttrInfo.getId() == null){
            //如果属性的id为null,是新增
            saveInfoAndValue(baseAttrInfo);
        }else {
            update(baseAttrInfo);
        }
    }
    //修改属性
    private void update(BaseAttrInfo baseAttrInfo) {
        //1.属性名可能会修改，先修改属性名
        baseAttrInfoMapper.updateById(baseAttrInfo);
        //2.创建一个集合收集有id的值
        List<Long> vids = new ArrayList<>();
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        for (BaseAttrValue baseAttrValue : attrValueList) {
            Long id = baseAttrValue.getAttrId();
            //如果id不为空 说明是修改
            if (id != null){
                vids.add(id);
            }
        }
        //先删除：为什么要先删除，因为删除要对比提交过来的id,没有提交过来但是数据库中有的要删除，
        //新增的也没有id，如果先新增了，再去删除，会将新增的误删
        if (vids.size() > 0){
            //部分删除：一个id都没有，就是说没有要修改的，数据库全删，可能会有新增的
            QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("attr_id", baseAttrInfo.getId());//查出属性对应的所有的值
            queryWrapper.notIn("id",vids);//得出不在修改了id中的值，删除
            baseAttrValueMapper.delete(queryWrapper);
        }else {
            //全部删除
            QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("attr_id", baseAttrInfo.getId());//查出属性对应的所有的值
            baseAttrValueMapper.delete(queryWrapper);
        }

        //修改和新增
        for (BaseAttrValue baseAttrValue : attrValueList) {
            //如果没有id的话就新增
            if (baseAttrValue.getId() == null){
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }

            //如果有id的话就是修改
            if (baseAttrValue.getId() != null){
                baseAttrValueMapper.updateById(baseAttrValue);
            }
        }
    }

    //添加保存属性的方法
    private void saveInfoAndValue(BaseAttrInfo baseAttrInfo) {
        //添加属性
        baseAttrInfoMapper.insert(baseAttrInfo);
        //添加属性对应的值
        //获得添加到属性的id值，在添加属性值得时候根据id添加
        Long id = baseAttrInfo.getId();
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        for (BaseAttrValue baseAttrValue : attrValueList) {
             baseAttrValue.setAttrId(id);
             baseAttrValueMapper.insert(baseAttrValue);
        }
    }
}




