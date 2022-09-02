package com.atguigu.gmall.product;

import com.atguigu.gmall.product.init.SkuIdBloomInitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BloomTest {
    @Autowired
    SkuIdBloomInitService skuIdBloomInitService;
    @Test
    void  Test(){
        skuIdBloomInitService.initSkuBloom();

    }
}
