package cn.easykits.crm.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售漏斗：线索 → 有效线索 → 商机 → 赢单 → 订单。
 * <p>count / amount 为各环节绝对量；conversionRate 为相对上一环节的数量转化率（%）。
 */
@Data
public class FunnelVO {

    private List<Stage> stages;

    @Data
    public static class Stage {

        /** 环节编码：lead / converted / opportunity / won / order */
        private String code;

        private String label;

        private Long count;

        /** 金额（线索 / 有效线索环节无金额，返回 null） */
        private BigDecimal amount;

        /** 相对上一环节的数量转化率（%），首个环节为 null */
        private BigDecimal conversionRate;
    }
}
