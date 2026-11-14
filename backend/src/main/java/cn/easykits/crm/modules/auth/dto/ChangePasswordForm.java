package cn.easykits.crm.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 当前登录用户自助修改密码。 */
public record ChangePasswordForm(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度须为 6-32 位") String newPassword,
        @NotBlank(message = "确认密码不能为空") String confirmPassword) {
}
