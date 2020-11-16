package cn.lx.qiniu.controller;

import cn.lx.qiniu.util.QiniuUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * cn.lx.qiniu.controller
 *
 * @Author Administrator
 * @date 17:04
 */

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Value("${qiniu.accessKey}")
    private String accessKey;

    @Value("${qiniu.secretKey}")
    private String secretKey;

    @Value("${qiniu.bucketName}")
    private String bucketName;

    @Value("${qiniu.externaLink}")
    private String externaLink;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        String fileName = QiniuUtils.upload(accessKey, secretKey, bucketName, externaLink, file);
        if (fileName == null) {
            return "上传失败";
        }
        return fileName;
    }
}