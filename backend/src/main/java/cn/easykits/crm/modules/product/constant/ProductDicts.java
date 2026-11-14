package cn.easykits.crm.modules.product.constant;

/**
 * 产品域状态字典（集中放标签，避免各 Service 散落硬编码）。
 */
public final class ProductDicts {

    private ProductDicts() {
    }

    /** 产品状态：0 草稿 / 1 上架 / 2 停用 */
    public static String productStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "上架";
            case 2 -> "停用";
            default -> String.valueOf(status);
        };
    }

    /** 报价单状态：0 草稿 / 1 已提交 / 2 已确认 / 3 已作废 */
    public static String quoteStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已提交";
            case 2 -> "已确认";
            case 3 -> "已作废";
            default -> String.valueOf(status);
        };
    }
}
