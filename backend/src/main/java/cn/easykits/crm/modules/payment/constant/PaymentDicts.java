package cn.easykits.crm.modules.payment.constant;

/**
 * 交易域状态字典（回款计划 / 回款记录 / 开票 / 退款）。
 */
public final class PaymentDicts {

    private PaymentDicts() {
    }

    /** 回款计划：0 未核销 / 1 部分核销 / 2 已核销 */
    public static String planStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "未核销";
            case 1 -> "部分核销";
            case 2 -> "已核销";
            default -> String.valueOf(status);
        };
    }

    /** 回款记录：0 未核销 / 1 已核销 */
    public static String writeoffStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "未核销";
            case 1 -> "已核销";
            default -> String.valueOf(status);
        };
    }

    /** 开票：0 待开票 / 1 已开票 / 2 已作废 */
    public static String invoiceStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "待开票";
            case 1 -> "已开票";
            case 2 -> "已作废";
            default -> String.valueOf(status);
        };
    }

    /** 退款：0 申请中 / 1 已审核 / 2 已退款 / 3 已驳回 */
    public static String refundStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "申请中";
            case 1 -> "已审核";
            case 2 -> "已退款";
            case 3 -> "已驳回";
            default -> String.valueOf(status);
        };
    }
}
