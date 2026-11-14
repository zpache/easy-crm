package cn.easykits.crm.modules.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同创建 / 更新表单。contractNo 留空由服务端生成。
 */
@Data
public class ContractForm {

    /** 合同编号；留空自动生成 CT-yyyyMMddHHmmss-xxxx */
    @Size(max = 64, message = "合同编号不能超过 64 个字符")
    private String contractNo;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    private Long opportunityId;

    @NotNull(message = "合同金额不能为空")
    @DecimalMin(value = "0.00", message = "合同金额不能为负数")
    private BigDecimal amount;

    private LocalDate signDate;

    /** 0 草稿 / 1 生效 / 2 已归档 / 3 已作废 */
    private Integer status;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
