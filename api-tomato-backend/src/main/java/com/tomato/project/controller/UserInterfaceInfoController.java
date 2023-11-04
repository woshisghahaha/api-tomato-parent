package com.tomato.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.tomato.apicommon.common.BaseResponse;
import com.tomato.apicommon.common.DeleteRequest;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.common.ResultUtils;
import com.tomato.apicommon.constant.UserConstant;
import com.tomato.apicommon.model.dto.userinterfaceinfo.UserInterfaceInfoAddRequest;
import com.tomato.apicommon.model.dto.userinterfaceinfo.UserInterfaceInfoEditRequest;
import com.tomato.apicommon.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.tomato.apicommon.model.entity.InterfaceInfo;
import com.tomato.apicommon.model.entity.User;
import com.tomato.apicommon.model.entity.UserInterfaceInfo;
import com.tomato.apicommon.model.vo.LoginUserVO;
import com.tomato.project.annotation.AuthCheck;
import com.tomato.project.exception.BusinessException;
import com.tomato.project.exception.ThrowUtils;
import com.tomato.project.service.InterfaceInfoService;
import com.tomato.project.service.UserInterfaceInfoService;
import com.tomato.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户接口管理
 *
 * @author tomato
 */
@RestController
@RequestMapping("/userInterfaceInfo")
@Slf4j
public class UserInterfaceInfoController {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建，也就是开通接口
     *
     * @param userInterfaceInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUserInterfaceInfo(@RequestBody UserInterfaceInfoAddRequest userInterfaceInfoAddRequest, HttpServletRequest request) {
        if (userInterfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(userInterfaceInfoAddRequest, userInterfaceInfo);
        // 校验用户和接口是否存在，该步骤不能放在userInterfaceInfoService做，会和InterfaceInfoService形成循环依赖
        // 更新并不需要校验，因为根本不需要改接口和用户
        isLegal(userInterfaceInfo);

        boolean result = userInterfaceInfoService.addUserInterface(userInterfaceInfo);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);//保存失败报错
        long newUserInterfaceInfoId = userInterfaceInfo.getId();
        return ResultUtils.success(newUserInterfaceInfoId);
    }

    private void isLegal(UserInterfaceInfo userInterfaceInfo) {
        Long userId = userInterfaceInfo.getUserId();
        Long interfaceInfoId = userInterfaceInfo.getInterfaceInfoId();

        boolean userExists = userService.lambdaQuery().eq(User::getId, userId).exists();
        if(!userExists){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
        }
        boolean interfaceInfoExists = interfaceInfoService.lambdaQuery().eq(InterfaceInfo::getId, interfaceInfoId).exists();
        if(!interfaceInfoExists){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口不存在");
        }
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUserInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //User user = userService.getLoginUserBySession(request);
        LoginUserVO user = userService.getLoginUserByThreadLocal();
        long id = deleteRequest.getId();
        // 判断是否存在
        UserInterfaceInfo oldUserInterfaceInfo = userInterfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldUserInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldUserInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = userInterfaceInfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param userInterfaceInfoUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> UpdateUserInterfaceInfo(@RequestBody UserInterfaceInfoEditRequest userInterfaceInfoUpdateRequest) {
        if (userInterfaceInfoUpdateRequest == null || userInterfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(userInterfaceInfoUpdateRequest, userInterfaceInfo);
        // 参数校验
        userInterfaceInfoService.validUserInterfaceInfo(userInterfaceInfo, false);
        long id = userInterfaceInfoUpdateRequest.getId();
        // 判断是否存在
        UserInterfaceInfo oldUserInterfaceInfo = userInterfaceInfoService.getById(id);
        ThrowUtils.throwIf(oldUserInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = userInterfaceInfoService.updateById(userInterfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserInterfaceInfo> getUserInterfaceInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.getById(id);
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(userInterfaceInfo);
    }

    /**
     * 获取列表（管理员类）
     *
     * @param userInterfaceInfoQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserInterfaceInfo>> ListInterfaceByPage(@RequestBody UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest)
    {
        long current = userInterfaceInfoQueryRequest.getCurrent();
        long size = userInterfaceInfoQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<UserInterfaceInfo> userInterfaceInfoPage = userInterfaceInfoService.page(new Page<>(current, size),
                userInterfaceInfoService.getQueryWrapper(userInterfaceInfoQueryRequest));
        return ResultUtils.success(userInterfaceInfoPage);
    }

    // endregion

}
