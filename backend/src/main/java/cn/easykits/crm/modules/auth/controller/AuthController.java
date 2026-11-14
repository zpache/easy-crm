package cn.easykits.crm.modules.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.auth.dto.LoginRequest;
import cn.easykits.crm.modules.auth.dto.RefreshRequest;
import cn.easykits.crm.modules.auth.service.AuthService;
import cn.easykits.crm.modules.auth.vo.LoginVO;
import cn.easykits.crm.security.AdminAuthorizationService;
import cn.easykits.crm.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录认证、退出、令牌刷新。 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminAuthorizationService authorizationService;
    private final SysUserMapper sysUserMapper;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(LoginUserContext.require());
    }

    /** 退出登录：递增 tokenVersion，让该用户的 access / refresh 令牌全部立即失效。 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        LoginUser loginUser = LoginUserContext.require();
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, loginUser.userId())
                .setSql("token_version = token_version + 1"));
        return Result.ok();
    }

    /**
     * 用 refresh token 换取新的 access token（白名单端点，不需要登录态）。
     * 会回库校验账号仍启用且 tokenVersion 未变，因此改密 / 停用 / 登出后刷新同样会被拒绝。
     */
    @PostMapping("/refresh")
    public Result<String> refresh(@RequestBody(required = false) RefreshRequest request) {
        String refreshToken = request == null ? null : request.refreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        JwtTokenProvider.RefreshPayload payload = jwtTokenProvider.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new BizException(ResultCode.UNAUTHORIZED));
        if (!authorizationService.isTokenVersionValid(payload.userId(), payload.tokenVersion())) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        LoginUser loginUser = authorizationService.buildLoginUser(payload.userId());
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return Result.ok(jwtTokenProvider.createAccessToken(loginUser, payload.tokenVersion()));
    }
}
