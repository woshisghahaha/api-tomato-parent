package com.tomato.apicommon.constant;

/**
 * 用户常量
 *
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    //下面用于JWT存登录用户信息和解析JWT的信息
    String ID="id";

    /**
     * 用户昵称
     */
    String USERNAME="userName";

    /**
     * 用户头像
     */
    String USERAVATAR="userAvatar";

    /**
     * 用户简介
     */
    String USERPROFILE="userProfile";

    /**
     * 用户角色：user/admin/ban
     */
    String USERROLE="userRole";


    // endregion
}
