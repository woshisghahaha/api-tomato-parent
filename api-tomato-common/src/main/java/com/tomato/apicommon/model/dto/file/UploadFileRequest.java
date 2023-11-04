package com.tomato.apicommon.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传请求
 *
 * @author Tomato
 */
@Data
public class UploadFileRequest implements Serializable {

    /**
     * 业务
     */
    private String biz;

    /**
     * 上传者的ID
     */
    private Long useId;

    private static final long serialVersionUID = 1L;
}