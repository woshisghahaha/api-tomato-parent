package com.tomato.project.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.tomato.apicommon.common.BaseResponse;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.common.ResultUtils;
import com.tomato.apicommon.constant.UserConstant;
import com.tomato.apicommon.model.vo.LoginUserVO;
import com.tomato.apicommon.utils.JwtUtils;
import com.tomato.project.utils.UserHolder;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//自定义拦截器
@Component //当前拦截器对象由Spring创建和管理
@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {
    private static final String AUTHORIZATION="Authorization";
    //前置方式
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("preHandle .... ");
        //1.获取请求url
        //2.判断请求url中是否包含login，如果包含，说明是登录操作，放行，此处已在注册器里实现

        // cors跨域复杂请求会先发送一个方法为OPTIONS的预检请求，这个请求是用来验证本次请求是否安全的
        // 因为复杂请求可能对服务器数据产生副作用。例如delete或者put,都会对服务器数据进行修改,
        // 所以在请求之前都要先询问服务器，当前网页所在域名是否在服务器的许可名单中，
        // 服务器允许后，浏览器才会发出正式的请求，否则不发送正式请求。
        // 过滤器会把预请求当做真正的请求去判断，所以在过滤器判断token之前先判断是不是预请求OPTIONS
        // 预请求OPTIONS没携带我们的自定义请求头AUTHORIZATION，所以此时应该拦截
        // 如果不判断，会取不出请求头里的Authorization
        String token="";
        log.info("检查路径：{}",request.getRequestURI());

        // 真相是自定义拦截器和跨域拦截器冲突导致跨域拦截器失效，改用过滤器实现跨域处理就解决问题了
        // 下面代码保留，以防万一
        //如果是预请求或是登出请求，则应该放行
        if (request.getMethod().equals("OPTIONS")) {
            //因为后台的拦截器若将option请求进行权限拦截，真正的请求就将不进行发送
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        // 如果不是预请求，则取出请求头
        else{
            // 3.获取请求头中的令牌（token），下面这行解析需要和前端约定好
            token= request.getHeader(AUTHORIZATION).replace("Bearer ", "");
        }
        log.info("从请求头中获取的令牌：{}", token);

        //4.判断令牌是否存在，如果不存在，返回错误结果（未登录）
        if (!StringUtils.hasLength(token)) {
            log.info("Token不存在");
            ResponseForFrontend(response);
            return false;//不放行
        }

        //5.解析token，如果解析失败，返回错误结果（未登录）
        try {
            // 已经保存过，那么就不需要再解析JWT令牌了
            LoginUserVO user = UserHolder.getUser();
            if(user!=null)return true;
            // 已登录的用户，保存到UserHolder中
            Claims claims = JwtUtils.parseJWT(token);
            LoginUserVO loginUserVO=new LoginUserVO();
            loginUserVO.setId((Long) claims.get(UserConstant.ID));
            loginUserVO.setUserName((String) claims.get(UserConstant.USERNAME));
            loginUserVO.setUserRole((String) claims.get(UserConstant.USERROLE));
            loginUserVO.setUserAvatar((String) claims.get(UserConstant.USERAVATAR));
            UserHolder.saveUser(loginUserVO);
        } catch (Exception e) {
            log.info("令牌解析失败!");

            //创建响应结果对象
            ResponseForFrontend(response);

            return false;
        }

        //6.放行
        return true;
    }

    private static void ResponseForFrontend(HttpServletResponse response) throws IOException {
        //创建响应结果对象
        BaseResponse responseResult = ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        //把Result对象转换为JSON格式字符串 (fastjson是阿里巴巴提供的用于实现对象和json的转换工具类)
        String json = JSONObject.toJSONString(responseResult);
        //设置响应头（告知浏览器：响应的数据类型为json、响应的数据编码表为utf-8）
        response.setContentType("application/json;charset=utf-8");
        //响应
        response.getWriter().write(json);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("afterCompletion.......");
        //清理资源
        UserHolder.removeUser();
    }
}
