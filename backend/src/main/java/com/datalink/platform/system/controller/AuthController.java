package com.datalink.platform.system.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.system.dto.LoginRequest;
import com.datalink.platform.system.dto.LoginResponse;
import com.datalink.platform.system.entity.SysUser;
import com.datalink.platform.system.mapper.SysUserMapper;
import com.datalink.platform.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;

    /**
     * 登录：校验用户名密码，返回 JWT 令牌与用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return Result.ok(sysUserService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * 当前登录用户信息：从安全上下文取用户名，回显显示名与角色（token 为空）
     */
    @GetMapping("/me")
    public Result<LoginResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        SysUser user = sysUserService.findByUsername(authentication.getName());
        if (user == null) {
            return Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        List<String> roles = sysUserMapper.selectRoleCodes(user.getId());
        return Result.ok(new LoginResponse(null, user.getDisplayName(), roles == null ? Collections.emptyList() : roles));
    }
}
