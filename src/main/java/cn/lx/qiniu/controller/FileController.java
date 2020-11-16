package cn.lx.qiniu.controller;

import cn.lx.qiniu.util.QiniuUtils;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/list")
    public void list() {
        QiniuUtils.list(accessKey, secretKey, bucketName);
    }

    @DeleteMapping("/delete/{filename}")
    public void delete(@PathVariable("filename") String filename) {
        QiniuUtils.delete(accessKey, secretKey, bucketName,filename);
    }

    @GetMapping("/info/{filename}")
    public void info(@PathVariable("filename") String filename) {
        QiniuUtils.getFileInfo(accessKey, secretKey, bucketName,filename);
    }


    @PostMapping("/upToken")
    public String getUpToken() {
        Auth auth = Auth.create(accessKey, secretKey);
        StringMap putPolicy = new StringMap();
        putPolicy.put("callbackUrl", "http://33833g4w32.wicp.vip:49739/callback/upload");
        putPolicy.put("callbackBody", "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"bucket\":\"$(bucket)\",\"fsize\":$(fsize)}");
        putPolicy.put("callbackBodyType", "application/json");
        long expireSeconds = 3600;
        String upToken = auth.uploadToken(bucketName, null, expireSeconds, putPolicy);
        log.info("upToken:{}",upToken);

        return upToken;
    }


    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        String fileName = QiniuUtils.upload(accessKey, secretKey, bucketName, externaLink, file);
        if (fileName == null) {
            return "上传失败";
        }
        return fileName;
    }
}
