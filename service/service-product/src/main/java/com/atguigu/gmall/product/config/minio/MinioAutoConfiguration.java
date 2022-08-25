package com.atguigu.gmall.product.config.minio;

import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

@Configuration
public class MinioAutoConfiguration {

    @Autowired
    MinioProperties minioProperties;
    @Bean
    public MinioClient minioClient() throws Exception {
        //1、创建Minio客户端
        MinioClient minioClient = new MinioClient(
                minioProperties.getEndpoint(),
                minioProperties.accessKey,
                minioProperties.getSecreKey()
        );
        //判断，如果桶不存在，就创建一个桶
        String bucketName = minioProperties.getBucketName();
        if (!minioClient.bucketExists(bucketName)){
            minioClient.makeBucket(bucketName);
        }
        return minioClient;
    }
}
