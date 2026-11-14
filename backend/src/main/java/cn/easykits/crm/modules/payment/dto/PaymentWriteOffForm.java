package cn.easykits.crm.modules.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 回款核销表单：把一条回款记录核销到指定回款计划。
 */
@Data
public class PaymentWriteOffForm {

    @NotNull(message = "回款计划不能为空")
    private Long planId;
}
