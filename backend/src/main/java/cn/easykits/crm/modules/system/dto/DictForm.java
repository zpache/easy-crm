package cn.easykits.crm.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 字典类型新增 / 编辑表单。
 *
 * @param dictType 字典类型编码（仅新增时生效，编辑不可改）
 * @param name     字典名称
 * @param remark   备注
 * @param status   1 启用，0 停用
 */
public record DictForm(
        @NotBlank(message = "字典类型编码不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$", message = "编码必须为小写字母开头的字母/数字/下划线，2-64 位")
        String dictType,

        @NotBlank(message = "字典名称不能为空")
        @Size(max = 64, message = "字典名称不能超过 64 个字符")
        String name,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        Integer status
) {
}
