package cn.easykits.crm.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 字典项新增 / 编辑表单。
 *
 * @param label  显示名称（Excel 导入按此匹配）
 * @param value  存储值（数字码或英文码）
 * @param sortNo 排序，越小越前
 * @param status 1 启用，0 停用
 * @param remark 备注
 */
public record DictItemForm(
        @NotBlank(message = "字典项名称不能为空")
        @Size(max = 64, message = "字典项名称不能超过 64 个字符")
        String label,

        @NotBlank(message = "字典项值不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "值只能包含字母/数字/下划线/连字符，1-64 位")
        String value,

        Integer sortNo,

        Integer status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
