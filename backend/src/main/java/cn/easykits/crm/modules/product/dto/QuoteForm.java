package cn.easykits.crm.modules.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报价单创建 / 更新表单。
 * 强制约定：明细汇总（Σ 单价*数量-折扣）必须等于 totalAmount，不一致拒绝保存。
 * quoteNo 留空时由服务端生成。
 */
@Data
public class QuoteForm {

    /** 报价单编号；留空自动生成 QT-yyyyMMddHHmmss-xxxx */
    @Size(max = 64, message = "报价单编号不能超过 64 个字符")
    private String quoteNo;

    private Long opportunityId;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotNull(message = "总金额不能为空")
    @DecimalMin(value = "0.00", message = "总金额不能为负数")
    private BigDecimal totalAmount;

    /** 0 草稿 / 1 已提交 / 2 已确认 / 3 已作废 */
    private Integer status;

    /** 报价有效期（截止日） */
    private LocalDate validUntil;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

    @Valid
    @NotEmpty(message = "报价明细不能为空")
    private List<QuoteItemForm> items;
}
