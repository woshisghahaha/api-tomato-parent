package com.tomato.project.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tomato.apicommon.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.tomato.apicommon.model.entity.UserInterfaceInfo;

import java.util.List;

public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {
    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);
    QueryWrapper<UserInterfaceInfo> getQueryWrapper(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest);

    List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit);

    Boolean addUserInterface(UserInterfaceInfo userInterfaceInfo);
}
