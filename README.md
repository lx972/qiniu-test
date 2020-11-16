# 七牛云文件上传和文件管理

七牛云官网教程：`https://developer.qiniu.com/kodo/sdk/1239/java`

## 1 上传

上传有两种方式

（1）服务端直传：服务端直传是指客户利用七牛服务端SDK从服务端直接上传文件到七牛云，交互的双方一般都在机房里面，所以服务端可以自己生成上传凭证，然后利用SDK中的上传逻辑进行上传，最后从七牛云获取上传的结果，这个过程中由于双方都是业务服务器，所以很少利用到上传回调的功能，而是直接自定义`returnBody`来获取自定义的回复内容。

（2）客户端直传：这种方式就是浏览器直接上传到七牛云，但是在上传前，先去你自己的服务器中获得一个签名，签名中就包含了身份识别。有了这个，七牛云就能将资源存储到对应的用户空间下。

下面这张图就是七牛云官网给出的编程模型。

![](image\FkPZ31ECmtGnEisOahMKc5kQkuRr.png)

### 1.1服务端直传

根据七牛云官网给出的字节数组上传案例改写过来的

`https://developer.qiniu.com/kodo/sdk/1239/java#upload-config`

```java
/**
     * 服务器直传
     *
     * @param accessKey
     * @param secretKey
     * @param bucket
     * @param externaLink 七牛云的外链域名
     * @param file
     * @return
     */
public static String upload(String accessKey, String secretKey, String bucket, String externaLink, MultipartFile file){
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
    //文件后缀名
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
```

### 1.2 客户端直传

#### 1.2.1 准备工作

带回调业务服务器的凭证：有时候客户端需要在文件上传到七牛之后，从业务服务器获取相关的信息，这个时候就要用到七牛的上传回调及相关回调参数的设置。

```java
String accessKey = "access key";
String secretKey = "secret key";
String bucket = "bucket name";

Auth auth = Auth.create(accessKey, secretKey);
StringMap putPolicy = new StringMap();
//回调的地址，你自己服务的地址，七牛云会将你上传的资源的信息通知到这个地址
putPolicy.put("callbackUrl", "http://api.example.com/qiniu/upload/callback");
//回调通知的json数据格式
putPolicy.put("callbackBody", "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"bucket\":\"$(bucket)\",\"fsize\":$(fsize)}");
//回调数据的格式
putPolicy.put("callbackBodyType", "application/json");
//凭证的过期时间
long expireSeconds = 3600;
//生成凭证
String upToken = auth.uploadToken(bucket, null, expireSeconds, putPolicy);
System.out.println(upToken);

```

在使用了上传回调的情况下，客户端收到的回复就是业务服务器响应七牛的JSON格式内容。

```properties
#回调内容示例如下
key=2020-10-1-15-52-4-768.png, hash=Fk-zGb08eAJvR5QTkCjBamCBo1s4, bucket=lx1, fsize=39262
```

上面就是生成凭证的代码了，我们在提交上传表单的时候，先请求自己的服务器，获取凭证，然后携带凭证上传

下面是官网给出的前端代码

```html
<form method="post" action="http://upload.qiniup.com/"
 enctype="multipart/form-data">
  <input name="key" type="hidden" value="<resource_key>">
  <input name="x:<custom_name>" type="hidden" value="<custom_value>">
  <input name="token" type="hidden" value="<upload_token>">
  <input name="crc32" type="hidden" />
  <input name="accept" type="hidden" />
  <input name="file" type="file" />
  <input type="submit" value="上传文件" />
</form>
```

![](image\Snipaste_2020-11-16_16-10-41.png)

#### 1.2.2 客户端直传实现

（1）加入Thymeleaf

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

（2）前端页面(放在resources下templates中)

```html
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>upload</title>
    </head>
    <body>
        <form id="upload" method="post" action="http://upload-z2.qiniup.com"
              enctype="multipart/form-data">
            <!--文件名-->
            <input name="key" type="hidden" id="key">

            <!--签名-->
            <input name="token" type="hidden" id="token">

            <input name="file" id="file" type="file" />
        </form>
        <button onclick="upload()">上传文件</button>

    </body>
    <!--jquery放在resources下static中-->
    <script src="/jquery/jquery-1.12.4.min.js"></script>
    <script>

        //上传
        function upload() {
            //获取上传文件的文件名
            var originalFileName=$("#file").val();
            console.log("originalFileName:"+originalFileName)
            //获取文件的后缀名
            var suffix=originalFileName.substring(originalFileName.lastIndexOf("."),originalFileName.length)
            var date=new Date()
            //文件名为日期
            var prefix=date.getFullYear()+"-"+date.getMonth()+"-"+date.getDay()
            +"-"+date.getHours()+"-"+date.getMinutes()+"-"+date.getSeconds()+"-"+date.getMilliseconds()

            //将文件名设置到表单中，一起提交上去
            $("#key").val(prefix+suffix)
            console.log("key:"+prefix+suffix)
            //这个异步请求是用来获取上传凭证的
            $.ajax({
                url: "/file/upToken",
                type: "post",
                success:function (resp) {
                    if (resp!=null){
                        console.log("resp:"+resp)
                        $("#token").val(resp)
                        //表单提交
                        $("#upload").submit()
                    }else {
                        alert("获取token失败")
                    }
                }
            })
        }
    </script>
</html>
```

接下来，我们为该页面添加一个控制器

```java
/**
 *
 * @EnableWebMvc 该注解有一套自己的springmvc默认配置，使用它会覆盖springboot的mvc默认配置
 * @Author Administrator
 * @date 11:27
 */
//@EnableWebMvc
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    /**
     * Configure simple automated controllers pre-configured with the response
     * status code and/or a view to render the response body. This is useful in
     * cases where there is no need for custom controller logic -- e.g. render a
     * home page, perform simple site URL redirects, return a 404 status with
     * HTML content, a 204 with no content, and more.
     *
     * @param registry
     * @see ViewControllerRegistry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/view/upload").setViewName("upload");
    }
}
```

（3）七牛云回调

需要公网，你可以使用内网穿透工具

```java
@RestController
@RequestMapping("/callback")
@Slf4j
public class CallbackController {

    @RequestMapping("/upload")
    public String upload(@RequestBody QiniuCallbackBody body) {
        log.info("QiniuCallbackBody:{}",body.toString());
        //做些什么
        return null;
    }
}

```

与之对应的vo

```java
@Data
public class QiniuCallbackBody {

    private String key;

    private String hash;

    private String bucket;

    private String fsize;
}
```

(4)  上传凭证

```java
@PostMapping("/upToken")
public String getUpToken() {
    Auth auth = Auth.create(accessKey, secretKey);
    StringMap putPolicy = new StringMap();
    //公网
    putPolicy.put("callbackUrl", "http://33833g4w32.wicp.vip:49739/callback/upload");
    putPolicy.put("callbackBody", "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"bucket\":\"$(bucket)\",\"fsize\":$(fsize)}");
    putPolicy.put("callbackBodyType", "application/json");
    long expireSeconds = 3600;
    String upToken = auth.uploadToken(bucketName, null, expireSeconds, putPolicy);
    log.info("upToken:{}",upToken);

    return upToken;
}
```

#### 1.2.3 测试

访问`http://localhost:9090/view/upload`



![](image\Snipaste_2020-11-16_16-45-24.png)

选择文件，然后点击上传文件按钮，控制台会打印如下内容，然后一闪而过

![](image\Snipaste_2020-11-16_16-48-30.png)

页面自动跳转

![](image\Snipaste_2020-11-16_16-50-12.png)

idea控制台打印

```json
2020-11-16 16:43:05.423  INFO 47448 --- [nio-9090-exec-5] c.l.qiniu.controller.CallbackController  : QiniuCallbackBody:QiniuCallbackBody(key=2020-10-1-16-43-3-343.png, hash=Fg_iscmKkXKEdbw1EMWUxxDaQkpN, bucket=lx1, fsize=82592)
2020-11-16 16:47:52.931  INFO 47448 --- [nio-9090-exec-1] cn.lx.qiniu.controller.FileController    : upToken:cCDDLE7qNzBcbAlpkOhqspM6Snim1UIREGTAfdcu:xM3tHR4MISJW8-l5Bc5hlam-PBw=:eyJjYWxsYmFja0JvZHlUeXBlIjoiYXBwbGljYXRpb24vanNvbiIsInNjb3BlIjoibHgxIiwiY2FsbGJhY2tVcmwiOiJodHRwOi8vMzM4MzNnNHczMi53aWNwLnZpcDo0OTczOS9jYWxsYmFjay91cGxvYWQiLCJkZWFkbGluZSI6MTYwNTUyMDA3MiwiY2FsbGJhY2tCb2R5Ijoie1wia2V5XCI6XCIkKGtleSlcIixcImhhc2hcIjpcIiQoZXRhZylcIixcImJ1Y2tldFwiOlwiJChidWNrZXQpXCIsXCJmc2l6ZVwiOiQoZnNpemUpfSJ9
2020-11-16 16:47:53.660  INFO 47448 --- [nio-9090-exec-3] c.l.qiniu.controller.CallbackController  : QiniuCallbackBody:QiniuCallbackBody(key=2020-10-1-16-47-52-920.png, hash=Fg_iscmKkXKEdbw1EMWUxxDaQkpN, bucket=lx1, fsize=82592)
```

去七牛云对象存储查看，发现上传成功

## 2 文件管理

### 2.1 获取文件信息

```java
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

```

### 2.2 删除文件

```java
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
```

### 2.3 获取文件列表

```java
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
```

