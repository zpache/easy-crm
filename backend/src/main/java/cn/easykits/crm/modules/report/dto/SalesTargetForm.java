package cn.easykits.crm.modules.report.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售目标创建 / 更新表单。唯一性口径：(target_month, dept_id, owner_id)。
 */
@Data
public class SalesTargetForm {

    @NotBlank(message = "目标年月不能为空")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "目标年月格式必须为 YYYY-MM")
    private String targetMonth;

    /** NULL = 不限部门 */
    private Long deptId;

    /** NULL = 部门整体目标 */
    private Long ownerId;

    @NotNull(message = "目标金额不能为空")
    @DecimalMin(value = "0.00", message = "目标金额不能为负数")
    private BigDecimal targetAmount;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
