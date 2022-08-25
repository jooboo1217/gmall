package com.atguigu.gmall.product.config.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    String endpoint;//http://192.168.6.200:9000
    String accessKey;//admin
    String secreKey;//admin123456
    String bucketName;//gmall
}
