package com.tomato.apitomatoclientsdk.model;

/**
 * 用户常量
 *
 * @author Tomato
 */
public interface APIHeaderConstant {
    /**
     * 用户accessKey
     */
    String ACCESSKEY = "accessKey";
    /**
     * 调用接口的id
     */
    String API_ID = "id";
    /**
     * 随机数，使用后失效，保证接口只能使用一次
     */
    String NONCE = "nonce";
    /**
     * 时间戳，保证是一定时间内的请求
     */
    String TIMESTAMP = "timestamp";
    /**
     * 签名，对比看看有没有资格
     */
    String SIGN ="sign";
    /**
     * 参数列表
     */
    String BODY="body";
    //对于外部的接口，采用下面的方式进行判断
    /**
     * 调用方式
     */
    String METHOD="method";
    /**
     * host
     */
    String PATH="path";
    /**
     * url
     */
    String URL="url";


    // endregion
}
