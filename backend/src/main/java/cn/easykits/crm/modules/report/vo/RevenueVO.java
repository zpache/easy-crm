package cn.easykits.crm.modules.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 回款 / 收入分析：按月份合并「回款计划 / 实收 / 已开票 / 已退款」。
 */
@Data
public class RevenueVO {

    private List<Row> rows;

    private BigDecimal totalPlanAmount;

    private BigDecimal totalPaymentAmount;

    private BigDecimal totalInvoiceAmount;

    private BigDecimal totalRefundAmount;

    /** 净流入 = 实收 - 退款 */
    private BigDecimal totalNetAmount;

    @Data
    public static class Row {

        private String month;

        /** 计划回款金额 */
        private BigDecimal planAmount;

        /** 实收金额 */
        private BigDecimal paymentAmount;

        /** 已开票金额 */
        private BigDecimal invoiceAmount;

        /** 已退款金额 */
        private BigDecimal refundAmount;

        /** 净流入 = 实收 - 退款 */
        private BigDecimal netAmount;

        /** 计划完成率 = 实收 / 计划（计划为 0 时 null） */
        private BigDecimal completionRate;
    }
}
