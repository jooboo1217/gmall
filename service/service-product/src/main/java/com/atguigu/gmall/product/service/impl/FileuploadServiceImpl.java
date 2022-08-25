package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.product.config.minio.MinioProperties;
import com.atguigu.gmall.product.service.FileuploadService;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Service
public class FileuploadServiceImpl implements FileuploadService {


    @Autowired
    MinioProperties minioProperties;
    @Autowired
    MinioClient minioClient;

    @Override
    public String upload(MultipartFile file) throws Exception {
        //1.创建Minclient，已抽取为配置，直接引入

        //2.判断桶是否存在
        boolean gmall = minioClient.bucketExists(minioProperties.getBucketName());
        if (!gmall){
        //桶不存在 创建桶
            minioClient.makeBucket(minioProperties.getBucketName());
        }
        //3、给桶里面上传文件
        //objectName：对象名，上传的文件名
        String name = file.getName(); //input的name的名
        //得到一个唯一文件名
        String dateStr = DateUtil.formatDate(new Date());
        String filename = UUID.randomUUID().toString().replace("-","")
                + "_" + file.getOriginalFilename(); //原始文件名
        //dadsajdajlk_aaa.png

        InputStream inputStream = file.getInputStream();
        String contentType = file.getContentType();

        //文件上传参数：long objectSize, long partSize

        PutObjectOptions options = new PutObjectOptions(file.getSize(),-1L);
        //默认都是二进制，必须修改成对应的图片等类型
        options.setContentType(contentType);
        //4、文件上传
        minioClient.putObject(
                minioProperties.getBucketName(),
                dateStr+"/"+filename, //自己指定的唯一名
                inputStream,
                options
        );

        //返回刚才上传文件的可访问路径
        String url = minioProperties.getEndpoint()+"/"+minioProperties.getBucketName()+"/"+dateStr+"/"+filename;
        return url;
    }
}
