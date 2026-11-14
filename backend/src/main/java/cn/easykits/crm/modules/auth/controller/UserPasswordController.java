package cn.easykits.crm.modules.auth.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.modules.auth.dto.ChangePasswordForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户自助修改密码；修改后 tokenVersion 递增，所有旧令牌立即失效。 */
@Tag(name = "用户-密码")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserPasswordController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordForm form) {
        LoginUser loginUser = LoginUserContext.require();
        SysUser user = sysUserMapper.selectById(loginUser.userId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(form.oldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (!form.newPassword().equals(form.confirmPassword())) {
            throw new BizException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(form.newPassword(), user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(form.newPassword()))
                .setSql("token_version = token_version + 1"));
        return Result.ok();
    }
}
