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
 * 网易云音乐接口
 *
 */
@RestController
public class NetEaseController {
    /**
     * 热门音乐评论
     */
    @PostMapping("/api/comments.163")
    public BaseResponse<String> hotComments(HttpServletRequest request) {
        String url = "https://api.uomg.com/api/comments.163";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        System.out.println(body);
        try(HttpResponse httpResponse = HttpRequest.post(url)
                .body(body)
                .execute()){
        return ResultUtils.success(httpResponse.body());
        }
    }

    /**
     * 随机音乐
     */
    @PostMapping("/api/rand.music")
    public BaseResponse<String> randMusic(HttpServletRequest request) {
        String url = "https://api.uomg.com/api/rand.music";
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        try(HttpResponse httpResponse = HttpRequest.get(url + "?" + body)
                .execute()){
        return ResultUtils.success(httpResponse.body());
        }
    }
}
