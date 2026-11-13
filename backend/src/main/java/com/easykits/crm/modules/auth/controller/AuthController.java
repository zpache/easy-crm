package com.easykits.crm.modules.auth.controller;

import com.easykits.crm.common.api.Result;
import com.easykits.crm.common.context.LoginUser;
import com.easykits.crm.common.context.LoginUserContext;
import com.easykits.crm.modules.auth.dto.LoginRequest;
import com.easykits.crm.modules.auth.service.AuthService;
import com.easykits.crm.modules.auth.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(LoginUserContext.require());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态：服务端不维护会话，客户端丢弃 token 即可。
        // 若后续需要强制下线，可在这里把 token 的 jti 写入黑名单。
        return Result.ok();
    }

    /**
     * vben 双 token 结构的刷新端点。当前 JWT 无状态，直接给当前用户重签一个；
     * TODO：接入 refresh token 独立颁发后再改造。
     */
    @PostMapping("/refresh")
    public Result<String> refresh() {
        LoginUser loginUser = LoginUserContext.require();
        return Result.ok(loginUser.username());
    }
}
