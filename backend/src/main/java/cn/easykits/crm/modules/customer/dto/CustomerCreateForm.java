package cn.easykits.crm.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户创建 / 编辑表单（编辑不带负责人字段，负责人变更走公海/领取）。
 */
@Data
public class CustomerCreateForm {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称不能超过 128 个字符")
    private String name;

    /** 统一社会信用代码（查重组合条件之一） */
    @Size(max = 32, message = "信用代码不能超过 32 个字符")
    private String creditCode;

    private Integer industry;

    private Integer scale;

    private Integer region;

    private Integer level;

    /** 仅创建时可用；编辑场景负责人走公海/领取 */
    private Long ownerId;

    private Long deptId;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
