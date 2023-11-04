package com.tomato.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.constant.CommonConstant;
import com.tomato.apicommon.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.tomato.apicommon.model.entity.UserInterfaceInfo;
import com.tomato.apicommon.model.vo.LoginUserVO;
import com.tomato.apicommon.utils.SqlUtils;
import com.tomato.project.exception.BusinessException;
import com.tomato.project.exception.ThrowUtils;
import com.tomato.project.mapper.UserInterfaceInfoMapper;
import com.tomato.project.service.UserInterfaceInfoService;
import com.tomato.project.utils.UserHolder;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* @author tomato
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service实现
*/
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
    implements UserInterfaceInfoService {


    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userInterfaceInfo.getUserId();
        Long interfaceInfoId = userInterfaceInfo.getInterfaceInfoId();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(interfaceInfoId == null||userId==null, ErrorCode.PARAMS_ERROR);
        }

        UserInterfaceInfo isOwned = this.lambdaQuery()
                .eq(UserInterfaceInfo::getUserId, userId)
                .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfoId)
                .one();
        if (isOwned!=null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "该用户已经拥有该接口");
        }
    }


    /**
     * 获取查询包装类
     *
     * @param userInterfaceInfoQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<UserInterfaceInfo> getQueryWrapper(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest) {

        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        if (userInterfaceInfoQueryRequest == null) {
            return queryWrapper;
        }

        Long id = userInterfaceInfoQueryRequest.getId();
        Long userId = userInterfaceInfoQueryRequest.getUserId();
        Long interfaceInfoId = userInterfaceInfoQueryRequest.getInterfaceInfoId();
        Integer status = userInterfaceInfoQueryRequest.getStatus();
        String sortField = userInterfaceInfoQueryRequest.getSortField();
        String sortOrder = userInterfaceInfoQueryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(interfaceInfoId), "interfaceInfoId", interfaceInfoId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }



    @Override
    public List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit) {
        return userInterfaceInfoMapper.listTopInvokeInterfaceInfo(limit);
    }

    @Override
    public Boolean addUserInterface(UserInterfaceInfo userInterfaceInfo) {
        // 设置默认剩余次数和调用次数
        userInterfaceInfo.setLeftNum(200);
        userInterfaceInfo.setTotalNum(0);
        LoginUserVO loginUser = UserHolder.getUser();
        userInterfaceInfo.setUserId(loginUser.getId());
        this.validUserInterfaceInfo(userInterfaceInfo, true);
        //User loginUser = userService.getLoginUserBySession(request);
        return this.save(userInterfaceInfo);
    }
}




