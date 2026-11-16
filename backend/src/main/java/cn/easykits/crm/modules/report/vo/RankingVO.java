package cn.easykits.crm.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售排行：按负责人汇总赢单 / 订单 / 回款实绩，按赢单金额降序。
 */
@Data
public class RankingVO {

    private List<Row> rows;

    @Data
    public static class Row {

        private Integer rank;

        private Long ownerId;

        private String ownerName;

        /** 赢单数 */
        private Long wonCount;

        /** 赢单金额 */
        private BigDecimal wonAmount;

        /** 订单数 */
        private Long orderCount;

        /** 订单金额 */
        private BigDecimal orderAmount;

        /** 回款金额 */
        private BigDecimal paymentAmount;
    }
}
