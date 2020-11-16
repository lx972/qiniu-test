package cn.lx.qiniu.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
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
     * @throws IOException
     */
    public static String upload(String accessKey, String secretKey, String bucket, String externaLink, MultipartFile file){
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region2());
        //...其他参数参考类注释
        UploadManager uploadManager = new UploadManager(cfg);
        //...生成上传凭证，然后准备上传
        //String accessKey = "your access key";
        //String secretKey = "your secret key";
        //String bucket = "your bucket name";
        //默认不指定key的情况下，以文件内容的hash值作为文件名
        //String key= null;
        //byte[] uploadBytes = "hello qiniu cloud".getBytes("utf-8");
        byte[] uploadBytes = new byte[0];
        try {
            uploadBytes = file.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String key = UUID.randomUUID().toString()  + suffix;

        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        try {
            Response response = uploadManager.put(uploadBytes, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info(putRet.key);
            log.info(putRet.hash);
            return externaLink+"/"+key;
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
}
