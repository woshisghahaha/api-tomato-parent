package com.tomato.apitomatointerface.controller;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.tomato.apicommon.common.BaseResponse;
import com.tomato.apicommon.common.ResultUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 有意思的接口
 *
 */
@RestController
public class FunApiController {

    private static BaseResponse<String> invokeOuterApi(String url, String body) {
        try(HttpResponse httpResponse = HttpRequest.get(url + "?" + body)
                .execute()){
            return ResultUtils.success(httpResponse.body());
        }
    }
    /**
     * 随机头像
     */
    @PostMapping("/api/rand.avatar")
    public BaseResponse<String> randAvatar(HttpServletRequest request) {
        String url = "https://api.uomg.com/api/rand.avatar";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        return invokeOuterApi(url, body);
    }

    /**
     * 随机壁纸
     */
    @PostMapping("/api/sjbz")
    public BaseResponse<String> randImages(HttpServletRequest request) {
        String url = "http://api.btstu.cn/sjbz/api.php";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        return invokeOuterApi(url, body);
    }
    /**
     * 毒鸡汤
     */
    @PostMapping("/api/poison")
    public BaseResponse<String> poisonChicken(HttpServletRequest request) {
        String url = "http://api.btstu.cn/yan/api.php";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        return invokeOuterApi(url, body);
    }

    /**
     * 短网址生成
     */
    @PostMapping("/api/long2dwz")
    public BaseResponse<String> long2dwz(HttpServletRequest request) {
        String url = "https://api.uomg.com/api/long2dwz";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        return invokeOuterApi(url, body);
    }

    /**
     * 获取qq头像和名字，没有效果
     */
    @PostMapping("/api/QQname")
    public BaseResponse<String> QQname(HttpServletRequest request) {
        String url = "http://api.btstu.cn/qqxt/api.php";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        return invokeOuterApi(url, body);
    }
}
