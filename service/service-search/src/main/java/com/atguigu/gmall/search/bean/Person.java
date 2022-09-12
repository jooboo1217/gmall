package com.atguigu.gmall.search.bean;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "person",shards = 1,replicas = 1)
public class Person {
    @Id
    Integer id ;
    @Field(value = "first",type = FieldType.Keyword)
    String firstName ;
    @Field(value = "last",type = FieldType.Keyword)
    String lastName ;
    @Field(value = "age")
    Integer age ;
    @Field(value = "addr")
    String address ;
}
