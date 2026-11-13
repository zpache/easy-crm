package com.easykits.crm.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easykits.crm.common.api.ResultCode;
import com.easykits.crm.common.context.LoginUser;
import com.easykits.crm.common.exception.BizException;
import com.easykits.crm.entity.SysUser;
import com.easykits.crm.mapper.SysUserMapper;
import com.easykits.crm.modules.auth.dto.LoginRequest;
import com.easykits.crm.modules.auth.vo.LoginVO;
import com.easykits.crm.modules.log.service.LoginLogService;
import com.easykits.crm.security.AdminAuthorizationService;
import com.easykits.crm.security.AdminUserDetails;
import com.easykits.crm.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理后台登录：认证 + 签发 JWT + 登录日志。
 * <p>TODO（后续轮次补齐）：登录防爆破锁定（easy-mall 的 LoginGuardService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminAuthorizationService authorizationService;
    private final SysUserMapper sysUserMapper;
    private final LoginLogService loginLogService;

    public LoginVO login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException e) {
            loginLogService.record(request.username(), null, false, "账号已停用");
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            log.warn("登录失败: username={}", request.username(), e);
            loginLogService.record(request.username(), null, false, "账号或密码错误");
            // 统一文案，不泄露账号是否存在
            throw new BizException("账号或密码错误");
        }

        AdminUserDetails details = (AdminUserDetails) authentication.getPrincipal();
        // 回库构造完整登录主体（deptId / dataScope）
        LoginUser loginUser = authorizationService.buildLoginUser(details.getId());
        if (loginUser == null) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        String token = jwtTokenProvider.createToken(loginUser);

        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, details.getId())
                .set(SysUser::getLastLoginAt, LocalDateTime.now()));

        loginLogService.record(request.username(), details.getId(), true, null);

        return new LoginVO(token, token, "Bearer", jwtTokenProvider.getExpireSeconds(), loginUser);
    }
}
