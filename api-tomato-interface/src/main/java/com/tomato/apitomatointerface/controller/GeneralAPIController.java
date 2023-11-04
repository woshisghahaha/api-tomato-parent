package com.tomato.apitomatointerface.controller;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.tomato.apicommon.common.BaseResponse;
import com.tomato.apicommon.common.ResultUtils;
import com.tomato.apitomatoclientsdk.model.APIHeaderConstant;
import com.tomato.apitomatoclientsdk.model.MethodEnum;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class GeneralAPIController {
    @PostMapping("/api/general")
    public BaseResponse<String> GeneralAPI(HttpServletRequest request) {
        String url = request.getHeader(APIHeaderConstant.URL);
        String method = request.getHeader(APIHeaderConstant.METHOD);
        String body = URLUtil.decode(request.getHeader("body"), CharsetUtil.CHARSET_UTF_8);
        // 如果是get请求
        String result = null;
        if(method.equals(MethodEnum.GET.getValue())){
            try(HttpResponse httpResponse = HttpRequest.get(url + "?" + body).execute()) {
                result = httpResponse.body();
            }
        }
        else if(method.equals(MethodEnum.POST.getValue())){
            try(HttpResponse httpResponse = HttpRequest.post(url)
                            // 处理中文编码
                            .header("Accept-Charset", CharsetUtil.UTF_8)
                            // 传递参数
                            .body(body)
                            .execute())
            {
                result= httpResponse.body();
            }
        }
        else if(method.equals(MethodEnum.PUT.getValue())){
            try(HttpResponse httpResponse = HttpRequest.put(url)
                    // 处理中文编码
                    .header("Accept-Charset", CharsetUtil.UTF_8)
                    // 传递参数
                    .body(body)
                    .execute())
            {
                result= httpResponse.body();
            }
        }
        return ResultUtils.success(result);
    }
}
