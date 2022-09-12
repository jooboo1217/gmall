package com.atguigu.gmall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@EnableElasticsearchRepositories  //开启ES的自动仓库功能。
@SpringCloudApplication
public class ServiceSearchMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceSearchMainApplication.class,args);
    }
}
