package com.tomato.project.controller;

import cn.hutool.core.io.FileUtil;
import com.tomato.apicommon.common.BaseResponse;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.common.ResultUtils;
import com.tomato.apicommon.exception.BusinessException;
import com.tomato.apicommon.exception.ThrowUtils;
import com.tomato.apicommon.model.dto.file.UploadFileRequest;
import com.tomato.apicommon.model.entity.User;
import com.tomato.apicommon.model.enums.FileUploadBizEnum;
import com.tomato.apicommon.model.vo.LoginUserVO;
import com.tomato.project.oss.AliOSSUtils;
import com.tomato.project.service.UserService;
import com.tomato.project.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * 文件接口
 *
 * @author Tomato
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private AliOSSUtils aliOSSUtils;

    private static final String FILE_DELIMITER = ",";

    /**
     * 根据不同的业务进行单个文件上传
     *
     * @param multipartFile
     * @param uploadFileRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           UploadFileRequest uploadFileRequest, HttpServletRequest request) {
        log.info("进入方法体");
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        // 不存在此业务，抛异常
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        // 由于前端直接调用接口URL导致跨域不携带session，无法从session中取当前用户
        // User loginUser = userService.getLoginUser(request);
        // Long userId = loginUser.getId();
        // 从ThreadLocal中获取当前用户
        LoginUserVO user = UserHolder.getUser();
        // 未登录
        ThrowUtils.throwIf(user==null,ErrorCode.NOT_LOGIN_ERROR);
        // 如果当前登录用户和请求参数里的用户id不一致，证明是攻击，抛异常
        Long userId = uploadFileRequest.getUseId();
        //ThrowUtils.throwIf(userId==null,ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(!user.getId().equals(userId),ErrorCode.PARAMS_ERROR);
        // 文件目录：根据业务、用户来划分，最后以文件名结尾
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("%s/%s/%s", fileUploadBizEnum.getValue(),userId , filename);
        //File file = null;
        try {
            // 上传文件,调用createTempFile()方法创建一个临时文件，并将上传文件的内容使用transferTo写入该临时文件中。
            /*
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            */
            // 我们知道当前对象是Controller，所以直接强转
            FileController proxy = (FileController) AopContext.currentProxy();
            log.info("调用方法中");
            // 直接调用该函数的对象是this，是当前对象，而不是代理对象，所以事务会失效
            String file = proxy.uploadFileByBiz(multipartFile, fileUploadBizEnum, userId, filepath);
            log.info("添加成功 访问地址：{}", file);

            // 返回可访问地址
            return ResultUtils.success(file);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }

    // 进行事务操作
    // 这样事务会失效
    @Transactional
    public String uploadFileByBiz(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum, Long useId, String filepath) throws IOException {
        //如果是头像上传业务，那么要马上更新头像
        if(FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)){
            // 提前获取URL用于更新数据库
            String url = aliOSSUtils.URL(filepath);
            User user = userService.getById(useId);
            user.setUserAvatar(url);
            userService.updateById(user);
            log.info("进入头像上传");
        }
        return aliOSSUtils.uploadFile2OSS(multipartFile, filepath);
    }


    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 5 * 1024 * 1024L;
        // 如果是上传文件的业务，判断后缀是否是图片
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    public BaseResponse<Map<String, Object>> uploadFiles(List<MultipartFile> files,UploadFileRequest uploadFileRequest, HttpServletRequest request) throws Exception {
        try {
            // 上传文件路径
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                // 上传并返回新文件名称
                BaseResponse<String> fileResponse = uploadFile(file, uploadFileRequest, request);
                urls.add(fileResponse.getData());
            }
            // 返回一大堆URL给前端，以","分割
            Map<String, Object> result = new HashMap<>();
            result.put("urls", StringUtils.join(urls, FILE_DELIMITER));
            return ResultUtils.success(result);
        } catch (Exception e) {
            throw new Exception("上传文件失败");
        }
    }
}
