package com.easykits.crm.modules.opportunity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 阶段配置新增 / 修改表单。
 */
@Data
public class StageForm {

    @NotBlank(message = "阶段编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "阶段编码需为大写字母、数字或下划线，且以字母开头")
    private String stageCode;

    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 64, message = "阶段名称不能超过 64 个字符")
    private String stageName;

    @NotNull(message = "排序不能为空")
    private Integer sortNo;

    @NotNull(message = "默认赢单率不能为空")
    @Min(value = 0, message = "赢单率范围 0-100")
    @Max(value = 100, message = "赢单率范围 0-100")
    private Integer defaultWinRate;

    /** 1 赢单终态 */
    private Integer isWon;

    /** 1 丢单终态 */
    private Integer isLost;

    /** 1 启用 / 0 停用 */
    private Integer enabled;
}
