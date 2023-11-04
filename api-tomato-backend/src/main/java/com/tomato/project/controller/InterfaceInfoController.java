package com.tomato.project.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.tomato.apicommon.common.*;
import com.tomato.apicommon.constant.CommonConstant;
import com.tomato.apicommon.constant.UserConstant;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoEditRequest;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.tomato.apicommon.model.entity.InterfaceInfo;
import com.tomato.apicommon.model.enums.InterfaceInfoStatusEnum;
import com.tomato.apicommon.model.vo.InterfaceInfoVO;
import com.tomato.apicommon.model.vo.LoginUserVO;
import com.tomato.project.annotation.AuthCheck;
import com.tomato.project.exception.BusinessException;
import com.tomato.project.exception.ThrowUtils;
import com.tomato.project.service.InterfaceInfoService;
import com.tomato.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 接口详情信息
 *
 * @author Tomato
 */
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;


    private final static Gson GSON = new Gson();


    // region 增删改查

    /**
     * 仅本人或管理员可编辑该接口
     *
     * @param oldInterfaceInfo
     */
    private void AuthCheckByInterface(InterfaceInfo oldInterfaceInfo) {
        LoginUserVO user = userService.getLoginUserByThreadLocal();
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 创建，用户或者管理员都可以创建接口
     *
     * @param interfaceInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);

        //校验添加的接口 只有管理员或者普通用户按照定好的接口路径path才可以进行添加
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        LoginUserVO loginUser = userService.getLoginUserByThreadLocal();
        //User loginUser = userService.getLoginUserBySession(request);

        interfaceInfo.setUserId(loginUser.getId());
        interfaceInfo.setRequestParamsRemark(JSONUtil.toJsonStr(interfaceInfoAddRequest.getRequestParamsRemark()));
        interfaceInfo.setResponseParamsRemark(JSONUtil.toJsonStr(interfaceInfoAddRequest.getResponseParamsRemark()));
        // 创建者直接就开通该接口
        boolean result = interfaceInfoService.addInterface(interfaceInfo);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);//保存失败报错
        long newInterfaceInfoId = interfaceInfo.getId();
        return ResultUtils.success(newInterfaceInfoId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //User user = userService.getLoginUserBySession(request);

        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        AuthCheckByInterface(oldInterfaceInfo);
        // 删除接口，并删除对应用户接口关系表
        boolean b = interfaceInfoService.removeByIdTranslator(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员可以更新的接口）
     *
     * @param interfaceInfoUpdateRequest
     * @return
     */
    @PostMapping("/update")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoEditRequest interfaceInfoUpdateRequest) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        long id = interfaceInfoUpdateRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑该接口
        AuthCheckByInterface(oldInterfaceInfo);
        // 下面函数中已经判断了接口是否合法
        boolean result = interfaceInfoService.updateInterfaceInfo(interfaceInfoUpdateRequest);
        return ResultUtils.success(result);
    }


    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<InterfaceInfoVO> getInterfaceInfoVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(interfaceInfoService.getInterfaceInfoVO(interfaceInfo));
    }

    /**
     * 获取所有接口列表，用于前端API商店页面的展示以及接口管理页面展示
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<InterfaceInfoVO>> listInterfaceInfoVOByPage(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest, HttpServletRequest request) {
        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 按时间排序
        interfaceInfoQueryRequest.setSortField("createTime");
        // 倒序排序
        interfaceInfoQueryRequest.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        // 非管理员只能查已开启的接口
        LoginUserVO user = userService.getLoginUserByThreadLocal();
        if (!userService.isAdmin(user)) {
            interfaceInfoQueryRequest.setStatus(1);
        }

        // 分页查询
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size),
                interfaceInfoService.getQueryWrapper(interfaceInfoQueryRequest));
        // 转为VO
        return ResultUtils.success(interfaceInfoService.getInterfaceInfoVOPage(interfaceInfoPage, request));
    }

    /**
     * 分页获取当前用户拥有的资源列表，也就是开通的接口，用于前端我的接口页面的展示
     *
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<IPage<InterfaceInfoVO>> ListMyInterfaceInfoVOByPage(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest,
                                                                            HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //long current  = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        interfaceInfoQueryRequest.setSortField("createTime");
        // 按创建时间倒序排序
        interfaceInfoQueryRequest.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        // 分页查询 直接在service层进行查询 不需要先分页
        // Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size),
        //      interfaceInfoService.getQueryWrapper(interfaceInfoQueryRequest));
        // 获取VO
        return ResultUtils.success(interfaceInfoService.getInterfaceInfoVOByUserIdPage(interfaceInfoQueryRequest));
    }

    // endregion

    /**
     * 分页获取当前用户创建的资源列表，也就是创建的接口，用于前端我的接口页面的展示
     *
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/own/list/page/vo")
    public BaseResponse<Page<InterfaceInfoVO>> ListOwnInterfaceInfoVOByPage(@RequestBody InterfaceInfoQueryRequest interfaceInfoQueryRequest,
                                                                            HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        interfaceInfoQueryRequest.setSortField("createTime");
        // 按创建时间倒序排序
        interfaceInfoQueryRequest.setSortOrder(CommonConstant.SORT_ORDER_DESC);
        // 限定只查创建者是当前用户的接口
        LoginUserVO loginUser = userService.getLoginUserByThreadLocal();
        interfaceInfoQueryRequest.setUserId(loginUser.getId());
        // 分页查询
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size),
                interfaceInfoService.getQueryWrapper(interfaceInfoQueryRequest));
        // 获取VO
        Page<InterfaceInfoVO> interfaceInfoVOPage = interfaceInfoService.getInterfaceInfoVOPage(interfaceInfoPage, request);
        return ResultUtils.success(interfaceInfoVOPage);
    }


    /**
     * 发布接口
     *
     * @param interfaceInfoInvokeRequest
     * @return
     */
    @PostMapping("/online")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long id = interfaceInfoInvokeRequest.getId();
        // 判断是否存在，也就是发布接口接口必须存在？
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        //仅本人或管理员可发布该接口
        AuthCheckByInterface(oldInterfaceInfo);
        // 如果下面这一步没抛异常，那么就可以成功上线接口，不需要返回值了
        //log.info("invoke Interface start.....");
        interfaceInfoService.getInvokeResult(interfaceInfoInvokeRequest, request, oldInterfaceInfo);
        //log.info("invoke Interface end.....");
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());//上线接口
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 下线接口
     *
     * @param idRequest
     * @return
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        //仅本人或管理员可下线该接口
        AuthCheckByInterface(oldInterfaceInfo);

        boolean result = interfaceInfoService.offlineInterface(id);
        return ResultUtils.success(result);
    }

    /**
     * 调用接口
     *
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = interfaceInfoInvokeRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(oldInterfaceInfo.getStatus().equals(InterfaceInfoStatusEnum.OFFLINE.getValue()), ErrorCode.PARAMS_ERROR, "接口已关闭");

        String invokeResult = interfaceInfoService.getInvokeResult(interfaceInfoInvokeRequest, request, oldInterfaceInfo);

        return ResultUtils.success(invokeResult);
    }

    // 下载SDK使用，未测试
    @GetMapping("/sdk")
    public void getSdk(HttpServletResponse response) throws IOException {
        // 获取要下载的文件
        org.springframework.core.io.Resource resource = new ClassPathResource("Tomato-Api-client-sdk-0.0.1-SNAPSHOT.jar");
        File file = resource.getFile();

        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());

        // 将文件内容写入响应
        try (InputStream in = Files.newInputStream(file.toPath());
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (IOException e) {
            // 处理异常
            e.printStackTrace();
        }
    }


}
