package cn.easykits.crm.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.auth.dto.LoginRequest;
import cn.easykits.crm.modules.auth.vo.LoginVO;
import cn.easykits.crm.modules.log.service.LoginLogService;
import cn.easykits.crm.security.AdminAuthorizationService;
import cn.easykits.crm.security.AdminUserDetails;
import cn.easykits.crm.security.JwtTokenProvider;
import cn.easykits.crm.security.LoginGuardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 管理后台登录：防爆破检查 + 认证 + 签发双 token + 登录日志。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminAuthorizationService authorizationService;
    private final SysUserMapper sysUserMapper;
    private final LoginLogService loginLogService;
    private final LoginGuardService loginGuardService;

    public LoginVO login(LoginRequest request) {
        loginGuardService.assertNotLocked(request.username());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException e) {
            loginLogService.record(request.username(), null, false, "账号已停用");
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        } catch (AuthenticationException e) {
            loginGuardService.onFailure(request.username());
            log.warn("登录失败: username={}", request.username());
            loginLogService.record(request.username(), null, false, "账号或密码错误");
            // 统一文案，不泄露账号是否存在
            throw new BizException("账号或密码错误");
        }

        loginGuardService.onSuccess(request.username());
        AdminUserDetails details = (AdminUserDetails) authentication.getPrincipal();
        // 回库构造完整登录主体（deptId / dataScope）
        LoginUser loginUser = authorizationService.buildLoginUser(details.getId());
        if (loginUser == null) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        SysUser user = sysUserMapper.selectById(details.getId());
        long tokenVersion = user == null || user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        String accessToken = jwtTokenProvider.createAccessToken(loginUser, tokenVersion);
        String refreshToken = jwtTokenProvider.createRefreshToken(details.getId(), tokenVersion);

        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, details.getId())
                .set(SysUser::getLastLoginAt, LocalDateTime.now()));

        loginLogService.record(request.username(), details.getId(), true, null);

        return new LoginVO(accessToken, refreshToken, "Bearer",
                jwtTokenProvider.getExpireSeconds(), loginUser);
    }
}
