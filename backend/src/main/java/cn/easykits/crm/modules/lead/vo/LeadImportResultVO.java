package cn.easykits.crm.modules.lead.vo;

import java.util.List;

/**
 * Excel 批量导入结果：逐行校验，错误行集中返回，不会因单行失败中断。
 */
public record LeadImportResultVO(int total, int successCount, int failCount, List<RowError> errors) {

    /**
     * 错误行：row 为 Excel 中的行号（含表头，从 1 开始）。
     */
    public record RowError(int row, String message) {
    }
}
