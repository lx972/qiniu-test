package cn.lx.qiniu.domian;

import lombok.Data;

/**
 * cn.lx.qiniu.domian
 *
 * @Author Administrator
 * @date 10:55
 */
@Data
public class QiniuCallbackBody {

    private String key;

    private String hash;

    private String bucket;

    private String fsize;
}
