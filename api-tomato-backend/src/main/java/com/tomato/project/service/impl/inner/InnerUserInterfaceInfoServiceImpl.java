package com.tomato.project.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.model.entity.UserInterfaceInfo;
import com.tomato.apicommon.service.InnerUserInterfaceInfoService;
import com.tomato.project.exception.BusinessException;
import com.tomato.project.service.UserInterfaceInfoService;
import com.tomato.project.service.UserService;
import com.tomato.project.service.impl.UserInterfaceInfoServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@DubboService
public class InnerUserInterfaceInfoServiceImpl implements InnerUserInterfaceInfoService {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Transactional //修改两次表，上事务
    @Override
    public boolean invokeCount(long interfaceInfoId, long userId) {
        // 查询接口是否存在
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
                .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfoId)
                .eq(UserInterfaceInfo::getUserId, userId)
                .one();
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口不存在");
        }
        // 修改调用次数
        return userInterfaceInfoService.lambdaUpdate()
                .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfoId)
                .eq(UserInterfaceInfo::getUserId, userId)
                .set(UserInterfaceInfo::getTotalNum, userInterfaceInfo.getTotalNum() + 1)
                .set(UserInterfaceInfo::getLeftNum, userInterfaceInfo.getLeftNum() - 1)
                .update();
    }

    @Override
    public UserInterfaceInfo hasLeftNum(Long interfaceId, Long userId) {
        return userInterfaceInfoService.lambdaQuery()
                .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceId)
                .eq(UserInterfaceInfo::getUserId, userId)
                .one();
    }

    @Override
    public Boolean addDefaultUserInterfaceInfo(Long interfaceId, Long userId) {
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        userInterfaceInfo.setUserId(userId);
        userInterfaceInfo.setInterfaceInfoId(interfaceId);
        userInterfaceInfo.setLeftNum(200);

        return userInterfaceInfoService.save(userInterfaceInfo);
    }

}
