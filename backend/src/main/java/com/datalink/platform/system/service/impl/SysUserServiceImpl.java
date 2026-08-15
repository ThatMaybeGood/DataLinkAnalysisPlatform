package com.datalink.platform.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.config.JwtUtil;
import com.datalink.platform.system.dto.LoginResponse;
import com.datalink.platform.system.entity.SysUser;
import com.datalink.platform.system.mapper.SysUserMapper;
import com.datalink.platform.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public LoginResponse login(String username, String rawPassword) {
        SysUser user = findByUsername(username);
        // 用户不存在或已停用，统一提示避免账号探测
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        // 密码校验
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        // 签发令牌并返回用户信息
        String token = jwtUtil.generateToken(user.getUsername());
        List<String> roles = sysUserMapper.selectRoleCodes(user.getId());
        return new LoginResponse(token, user.getDisplayName(), roles);
    }
}
