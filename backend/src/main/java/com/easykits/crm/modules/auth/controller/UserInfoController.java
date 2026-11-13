package com.easykits.crm.modules.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easykits.crm.common.api.Result;
import com.easykits.crm.common.context.LoginUser;
import com.easykits.crm.common.context.LoginUserContext;
import com.easykits.crm.entity.SysRole;
import com.easykits.crm.entity.SysUser;
import com.easykits.crm.mapper.SysRoleMapper;
import com.easykits.crm.mapper.SysUserMapper;
import com.easykits.crm.modules.auth.vo.UserInfoVO;
import com.easykits.crm.modules.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * vben 前端约定的用户信息接口。
 * 前端框架在刷新页面 / 换取路由权限时会调用这两个接口。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserInfoController {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final MenuService menuService;

    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        LoginUser loginUser = LoginUserContext.require();
        // avatar 不在 JWT 里，回库补一次
        SysUser sysUser = sysUserMapper.selectById(loginUser.userId());
        // 角色编码换角色名称，个人中心展示用
        List<String> roles = loginUser.roles() == null ? List.of() : loginUser.roles();
        List<String> roleNames = roles.isEmpty()
                ? List.of()
                : sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                                .in(SysRole::getCode, roles))
                        .stream()
                        .map(SysRole::getName)
                        .toList();
        return Result.ok(new UserInfoVO(
                loginUser.userId(),
                loginUser.username(),
                loginUser.nickname(),
                sysUser == null ? null : sysUser.getAvatar(),
                roles,
                roleNames,
                menuService.resolveHomePath(loginUser.userId())));
    }

    @GetMapping("/codes")
    public Result<List<String>> codes() {
        return Result.ok(menuService.listAuthCodesByUser(LoginUserContext.userId()));
    }
}
