package cn.easykits.crm.modules.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 阶段变更请求。目标为丢单阶段时 lostReason 必填；赢单时
 * actualCloseDate / actualAmount 缺省取今天 / 商机预计金额。
 */
@Data
public class StageChangeForm {

    @NotBlank(message = "目标阶段不能为空")
    private String targetStageCode;

    /** 赢单：实际成交日期（缺省今天） */
    private LocalDate actualCloseDate;

    /** 赢单：实际成交金额（缺省商机预计金额） */
    private BigDecimal actualAmount;

    /** 丢单：丢单原因字典值（必填） */
    private String lostReason;

    /** 丢单：竞争对手（可选） */
    private String competitor;
}
