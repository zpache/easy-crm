package cn.easykits.crm.modules.opportunity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商机更新表单（阶段变更走专用接口 PUT /{id}/stage）。
 */
@Data
public class OpportunityUpdateForm {

    @Size(max = 128, message = "商机名称不能超过 128 个字符")
    private String name;

    /** 关联联系人（可选） */
    private Long contactId;

    private BigDecimal amount;

    private LocalDate expectedCloseDate;
}
