package com.datalink.platform.system.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.system.dto.LoginRequest;
import com.datalink.platform.system.dto.LoginResponse;
import com.datalink.platform.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 登录：校验用户名密码，返回 JWT 令牌与用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return Result.ok(sysUserService.login(request.getUsername(), request.getPassword()));
    }
}
