package com.atguigu.gmall.product;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class rwTest {
    @Autowired
    BaseTrademarkMapper baseTrademarkMapper;
    @Test
    void testRW(){
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(12);
        System.out.println("baseTrademark = " + baseTrademark);

        BaseTrademark baseTrademark2 = baseTrademarkMapper.selectById(12);
        System.out.println("baseTrademark2 = " + baseTrademark2);

        BaseTrademark baseTrademark3 = baseTrademarkMapper.selectById(12);
        System.out.println("baseTrademark3 = " + baseTrademark3);

        BaseTrademark baseTrademark4 = baseTrademarkMapper.selectById(12);
        System.out.println("baseTrademark4 = " + baseTrademark4);
    }
    
    
    
    @Test
    public void testRw2(){
        //从数据库查询一值
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(12);
        System.out.println("baseTrademark = " + baseTrademark);

        baseTrademark.setTmName("苹果14");
        baseTrademarkMapper.updateById(baseTrademark);

        //让刚改完的下次查询强制走主库

//        BaseTrademark baseTrademark3 = baseTrademarkMapper.selectById(12);
//        System.out.println("baseTrademark = " + baseTrademark3);
        
        
        HintManager.getInstance().setWriteRouteOnly(); //强制走主库
        BaseTrademark baseTrademark2 = baseTrademarkMapper.selectById(12);
        System.out.println("baseTrademark = " + baseTrademark2);
        
    }
}
