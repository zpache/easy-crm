package cn.easykits.crm.modules.contract.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合同状态流转表单。
 */
@Data
public class ContractStatusForm {

    /** 目标状态：0 草稿 / 1 生效 / 2 已归档 / 3 已作废 */
    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
