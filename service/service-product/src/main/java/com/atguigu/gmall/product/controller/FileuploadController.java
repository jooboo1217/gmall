package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.FileuploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.util.UUID;

@RestController
@RequestMapping("/admin/product")
public class FileuploadController {
    @Autowired
    FileuploadService fileuploadService;

    @PostMapping("/fileUpload")
    public Result fileUpload(@RequestPart("file")MultipartFile file) throws Exception {

        String url = fileuploadService.upload(file);
        return Result.ok(url);
    }
}
