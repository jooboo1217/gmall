package com.atguigu.gmall.model.to;

import lombok.Data;

@Data
public class CategoryViewTo {
    //根据item  index需要的内容进行创建
    private Long category1Id;
    private String category1Name;
    private Long category2Id;
    private String category2Name;
    private Long category3Id;
    private String category3Name;
}
