package com.tomato.apicommon.model.dto.interfaceinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口调用请求
 *
 * @author Tomato
 */
@Data
public class InterfaceInfoInvokeRequest implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户请求参数
     */
    private String userRequestParams;
    //剩下的参数都可以通过接口ID查出来
    /**
     * 接口路径
     */
    private String path;
    /**
     * 接口地址
     */
    private String url;
    /**
     * 请求类型
     */
    private String method;


    private static final long serialVersionUID = 1L;
}