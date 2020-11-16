package cn.lx.qiniu.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * cn.lx.qiniu.util
 *
 * @Author Administrator
 * @date 17:11
 */
@Slf4j
public class QiniuUtils {


    /**
     * 服务器直传
     *
     * @param accessKey
     * @param secretKey
     * @param bucket
     * @param externaLink
     * @param file
     * @return
     */
    public static String upload(String accessKey, String secretKey, String bucket, String externaLink, MultipartFile file) {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);
        //...生成上传凭证，然后准备上传
        byte[] uploadBytes = new byte[0];
        try {
            uploadBytes = file.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String key = UUID.randomUUID().toString() + suffix;

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        try {
            Response response = uploadManager.put(uploadBytes, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info(putRet.key);
            log.info(putRet.hash);
            return externaLink + "/" + key;
        } catch (QiniuException ex) {
            Response r = ex.response;
            log.error(r.toString());
            try {
                log.error(r.bodyString());
            } catch (QiniuException ex2) {
                //ignore
            }
        }
        return null;
    }

    /**
     * 获取文件名对应文件信息
     *
     * @param accessKey
     * @param secretKey
     * @param bucket
     * @param filename
     */
    public static void getFileInfo(String accessKey, String secretKey, String bucket, String filename) {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        //...其他参数参考类注释
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            FileInfo fileInfo = bucketManager.stat(bucket, filename);
            log.info("hash:{}", fileInfo.hash);
            log.info("fsize:{}", fileInfo.fsize);
            log.info("mimeType:{}", fileInfo.mimeType);
            log.info("putTime:{}", fileInfo.putTime);
        } catch (QiniuException ex) {
            log.error(ex.response.toString());
        }
    }

    /**
     * 删除文件
     *
     * @param accessKey
     * @param secretKey
     * @param bucket
     * @param filename
     */
    public static void delete(String accessKey, String secretKey, String bucket, String filename) {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        //...其他参数参考类注释
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            bucketManager.delete(bucket, filename);
        } catch (QiniuException ex) {
            //如果遇到异常，说明删除失败
            log.error("code:{}", ex.code());
            log.error(ex.response.toString());
        }

    }

    /**
     * 获取文件列表
     *
     * @param accessKey
     * @param secretKey
     * @param bucket
     */
    public static void list(String accessKey, String secretKey, String bucket) {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        //...其他参数参考类注释
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);

        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 1000;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";

        //列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                log.info("开始");
                log.info("hash:{}", item.hash);
                log.info("fsize:{}", item.fsize);
                log.info("mimeType:{}", item.mimeType);
                log.info("putTime:{}", item.putTime);
                log.info("结束");
            }
        }

    }
}
