package com.atguigu.gmall.product.bloom;

import java.util.List;

//查询所有的数据，要存放到布隆过滤器中
public interface BloomDataQueryService {

    List queryData();
}
