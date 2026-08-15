package com.datalink.platform.system.service;

import com.datalink.platform.system.dto.LoginResponse;
import com.datalink.platform.system.entity.SysUser;

/**
 * 系统用户服务
 */
public interface SysUserService {

    /**
     * 按登录名查询用户
     */
    SysUser findByUsername(String username);

    /**
     * 登录校验：验证用户状态与密码，成功后签发 JWT 并返回用户信息
     */
    LoginResponse login(String username, String rawPassword);
}
