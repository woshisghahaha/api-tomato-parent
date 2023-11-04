package com.tomato.project.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.model.entity.User;
import com.tomato.apicommon.service.InnerUserService;
import com.tomato.project.exception.BusinessException;
import com.tomato.project.mapper.UserMapper;
import com.tomato.project.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Override
    public User getInvokeUser(String accessKey) {
        return userService.query()
                .eq("accessKey", accessKey)
                .one();
    }
}
