package cn.lx.qiniu.controller;

import cn.lx.qiniu.domian.QiniuCallbackBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * cn.lx.qiniu.controller
 *
 * @Author Administrator
 * @date 10:52
 */
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
