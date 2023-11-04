package com.tomato.apicommon.service;


import com.tomato.apicommon.model.entity.UserInterfaceInfo;

/**
 * 内部用户接口信息服务
 *
 */
public interface InnerUserInterfaceInfoService {

    /**
     * 调用接口统计
     *
     * @param interfaceInfoId 接口ID
     * @param userId          用户ID
     * @return boolean 是否执行成功
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    /**
     * 是否还有调用次数，其实就是在查对应接口用户关系表
     *
     * @param interfaceId 接口id
     * @param userId      用户id
     * @return UserInterfaceInfo 用户接口信息
     */
    UserInterfaceInfo hasLeftNum(Long interfaceId, Long userId);


    /**
     * 添加默认的用户接口信息
     *
     * @param interfaceId 接口id
     * @param userId      用户id
     * @return Boolean 是否添加成功
     */
    Boolean addDefaultUserInterfaceInfo(Long interfaceId, Long userId);



}
