package com.datalink.platform.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应：JWT 令牌 + 用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT 令牌 */
    private String token;

    /** 显示名称 */
    private String displayName;

    /** 角色编码列表 */
    private List<String> roles;
}
