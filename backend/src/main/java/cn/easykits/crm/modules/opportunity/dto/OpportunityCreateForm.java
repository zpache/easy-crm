package cn.easykits.crm.modules.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商机创建表单。必须关联客户；联系人可选。
 */
@Data
public class OpportunityCreateForm {

    @NotBlank(message = "商机名称不能为空")
    @Size(max = 128, message = "商机名称不能超过 128 个字符")
    private String name;

    @NotNull(message = "所属客户不能为空")
    private Long customerId;

    /** 关联联系人（可选） */
    private Long contactId;

    /** 预计金额 */
    private BigDecimal amount;

    /** 预计成交日期 */
    private LocalDate expectedCloseDate;
}
