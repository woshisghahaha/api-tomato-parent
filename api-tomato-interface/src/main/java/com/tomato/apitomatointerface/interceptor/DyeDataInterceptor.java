package com.tomato.apitomatointerface.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
/**
 * 染色数据拦截器
 */
@Slf4j
public class DyeDataInterceptor implements HandlerInterceptor {

    private static final String DYE_DATA_HEADER = "X-Dye-Data";
    private static final String DYE_DATA_VALUE = "tomato";

    /**
     * 调用接口时，如果发现是未经过网关染色的请求，不予响应
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获取请求头中的染色数据
        String dyeData = request.getHeader(DYE_DATA_HEADER);

        if (dyeData == null || !dyeData.equals(DYE_DATA_VALUE)) {
            // 如果染色数据不存在或者不匹配，则返回错误响应
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            log.info("染色数据不匹配" + dyeData);
            return false;
        }


        // 继续向下执行
        return true;
    }
}
