package com.tomato.filter;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.URLUtil;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.model.entity.InterfaceInfo;
import com.tomato.apicommon.model.entity.User;
import com.tomato.apicommon.model.entity.UserInterfaceInfo;
import com.tomato.apicommon.service.InnerInterfaceInfoService;
import com.tomato.apicommon.service.InnerUserInterfaceInfoService;
import com.tomato.apicommon.service.InnerUserService;
import com.tomato.apitomatoclientsdk.model.APIHeaderConstant;
import com.tomato.apitomatoclientsdk.utils.SignUtils;
import com.tomato.exception.BusinessException;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 全局过滤
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerUserService innerUserService;
    @DubboReference
    private InnerInterfaceInfoService interfaceInfoService;
    @DubboReference
    private InnerUserInterfaceInfoService userInterfaceInfoService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final List<String> IP_WHITE_LIST = Collections.singletonList("xxxxxxxxx");
    private static final String DYE_DATA_HEADER = "X-Dye-Data";
    private static final String DYE_DATA_VALUE = "tomato";
    private static final String KEY_PREFFIX = "User_access:nonce:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //请求日志
        ServerHttpRequest request = exchange.getRequest();
        String IP_ADDRESS = Objects.requireNonNull(request.getLocalAddress()).getHostString();
        String path = request.getPath().value();
//        String path = request.getPath().value();
        log.info("请求唯一标识：{}", request.getId());
        log.info("请求路径：{}", path);
//        log.info("请求参数：{}", request.getQueryParams());
        log.info("请求来源地址：{}", IP_ADDRESS);
//        log.info("请求来源地址：{}", request.getRemoteAddress());

        ServerHttpResponse response = exchange.getResponse();
        //访问控制 黑白名单
        if (!IP_WHITE_LIST.contains(IP_ADDRESS)) {
            log.info("进入黑名单");
            return handleNoAuth(response);
        }
        // 3. 用户鉴权 （判断 accessKey 和 secretKey 是否合法）
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst(APIHeaderConstant.ACCESSKEY);
        String timestamp = headers.getFirst(APIHeaderConstant.TIMESTAMP);
        String nonce = headers.getFirst(APIHeaderConstant.NONCE);
        String sign = headers.getFirst(APIHeaderConstant.SIGN);
        String body = URLUtil.decode(headers.getFirst(APIHeaderConstant.BODY), CharsetUtil.CHARSET_UTF_8);
        String id = headers.getFirst(APIHeaderConstant.API_ID);
        String URL = headers.getFirst(APIHeaderConstant.URL);
        String method = headers.getFirst(APIHeaderConstant.METHOD); 
        if (StringUtil.isEmpty(nonce)
                || StringUtil.isEmpty(sign)
                || StringUtil.isEmpty(timestamp)
                || StringUtil.isEmpty(method)
                || StringUtil.isEmpty(id)
                || StringUtil.isEmpty(URL)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求头参数不完整！");
        }

        long InterfaceInfoId = Integer.valueOf(id).longValue();
        // 通过 accessKey 查询是否存在该用户
        User invokeUser = innerUserService.getInvokeUser(accessKey);
        if (invokeUser == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "accessKey 不合法！");
        }
        // 判断随机数是否存在，防止重放攻击
        String key = KEY_PREFFIX + nonce;
        String existNonce = (String) redisTemplate.opsForValue().get(key);
        if (StringUtil.isNotBlank(existNonce)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求重复！");
        }
        // 时间戳 和 当前时间不能超过 5 分钟 (300000毫秒)
        long currentTimeMillis = System.currentTimeMillis() / 1000;
        long difference = currentTimeMillis - Long.parseLong(timestamp);
        if (Math.abs(difference) > 300000) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求超时！");
        }
        // 校验签名
        // 应该通过 accessKey 查询数据库中的 secretKey 生成 sign 和前端传递的 sign 对比
        String secretKey = invokeUser.getSecretKey();
        //log.info("网关得到tSecretKey：{}", secretKey);
        String serverSign = SignUtils.getSign(body, secretKey);
        if (!sign.equals(serverSign)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "签名错误！");
        }

        // 4. 请求的模拟接口是否存在？
        // 从数据库中查询接口是否存在，以及方法是否匹配（还有请求参数是否正确）
        InterfaceInfo interfaceInfo = null;
        try {
            interfaceInfo = interfaceInfoService.getInterfaceInfo(InterfaceInfoId, URL, method, path);
        } catch (Exception e) {
            log.error("getInvokeInterface error", e);
        }
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口不存在！");
        }
        Long interfaceInfoId = interfaceInfo.getId();
        Long userId = invokeUser.getId();

        // 查询剩余调用次数
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.hasLeftNum(interfaceInfoId, userId);
        // 接口未绑定用户，这里是不能完全信任前端，有可能客户未开通接口也调用？或者是管理员不开通直接调用？
        if (userInterfaceInfo == null) {
            Boolean save = userInterfaceInfoService.addDefaultUserInterfaceInfo(interfaceInfoId, userId);
            if (save == null || !save) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口绑定用户失败！");
            }
        }
        if (userInterfaceInfo != null && userInterfaceInfo.getLeftNum() <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用次数已用完！");
        }
        log.info("接口校验完毕，准备转发请求！");
        // 5. 请求转发，调用模拟接口
        // 5.1 去除无用请求头，不知道能不能提高一点速度.使用headers.remove会直接跑不通
        // 6. 响应日志
        return handleResponse(exchange, chain, interfaceInfoId, userId);
    }

    /**
     * 处理响应
     *
     * @param exchange
     * @param chain
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓存数据的工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();
            if (statusCode == HttpStatus.OK) {
                // 装饰，增强能力
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    // 等调用完转发的接口后才会执行
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 往返回值里写数据
                            // 拼接字符串
                            return super.writeWith(
                                    fluxBody.map(dataBuffer -> {
                                        // 7. 调用成功，接口调用次数 + 1 invokeCount，拿到调用成功的 请求头和返回头
                                        ServerHttpRequest invokeRequest = exchange.getRequest();
                                        ServerHttpResponse invokeResponse = exchange.getResponse();
                                        try {
                                            postHandler(invokeRequest, invokeResponse, interfaceInfoId, userId);
                                        } catch (Exception e) {
                                            log.error("invokeCount error", e);
                                        }
                                        byte[] orgContent = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(orgContent);
                                        DataBufferUtils.release(dataBuffer);//释放掉内存
                                        // 构建日志
                                        String data = new String(orgContent, StandardCharsets.UTF_8); //data
//                                        log.info("原始响应结果：" + data);
                                        if (invokeResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                                            data = String.format("{\"code\": %d,\"msg\":\"%s\",\"data\":\"%s\"}",
                                                    ErrorCode.NOT_FOUND_ERROR.getCode(), "接口请求路径不存在", "null");
                                            log.info("处理404后响应结果：" + data);
                                        }
                                        DataBufferFactory bufferFactory = invokeResponse.bufferFactory();
                                        // log.info("date长度：{}",data.length());
                                        // 打印日志
                                        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                                        // 告知客户端Body的长度，如果不设置的话客户端会一直处于等待状态不结束
                                        HttpHeaders headers = invokeResponse.getHeaders();
                                        headers.setContentLength(bytes.length);
                                        return bufferFactory.wrap(bytes);
                                    }));
                        } else {
                            // 8. 调用失败，返回一个规范的错误码
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 流量染色，只有染色数据才能被调用
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header(DYE_DATA_HEADER, DYE_DATA_VALUE)
                        .build();

                ServerWebExchange serverWebExchange = exchange.mutate()
                        .request(modifiedRequest)
                        .response(decoratedResponse)
                        .build();
                return chain.filter(serverWebExchange);
            }
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private void postHandler(ServerHttpRequest request, ServerHttpResponse response, Long interfaceInfoId, Long userId) {
        RLock lock = redissonClient.getLock("api:add_interface_num");
        if (response.getStatusCode() == HttpStatus.OK) {
            CompletableFuture.runAsync(() -> {
                if (lock.tryLock()) {
                    try {
                        addInterfaceNum(request, interfaceInfoId, userId);
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
    }

    private void addInterfaceNum(ServerHttpRequest request, Long interfaceInfoId, Long userId) {
        // 使用分布式锁实现接口总调用次数的增加
        // 判断nonce是否为空和判断请求剩余次数应该在转发调用前做
        String nonce = request.getHeaders().getFirst(APIHeaderConstant.NONCE);
        String key = KEY_PREFFIX + nonce;
        redisTemplate.opsForValue().set(key, 1, 5, TimeUnit.MINUTES);
        // 外面包了一层分布式锁，invokeCount应该不需要上分布式事务，因为只有一人会进行invokeCount的操作。
        userInterfaceInfoService.invokeCount(interfaceInfoId, userId);
    }

    /**
     * 响应没权限
     *
     * @param response
     * @return
     */
    public Mono<Void> handleNoAuth(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    public Mono<Void> handleInvokeError(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }
}
