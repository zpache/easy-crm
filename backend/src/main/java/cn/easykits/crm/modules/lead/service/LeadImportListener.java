package cn.easykits.crm.modules.lead.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import cn.easykits.crm.modules.lead.constant.LeadSource;
import cn.easykits.crm.modules.lead.dto.LeadImportRow;
import cn.easykits.crm.modules.lead.vo.LeadImportResultVO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 线索导入解析监听器：表头校验 + 逐行解析收集，单行失败只记错误不中断。
 * <p>只做「文件内可判定」的校验（必填 / 长度 / 文件内重复）；
 * 手机号 / 邮箱与库内查重由 LeadService 在解析完成后统一执行。
 */
@Slf4j
public class LeadImportListener extends AnalysisEventListener<LeadImportRow> {

    /** 模板第 1 列表头 */
    public static final String REQUIRED_HEADER = "线索名称";

    /** 表头不匹配标记（解析提前终止，由调用方转换为业务异常） */
    @Getter
    private boolean headerInvalid;
    @Getter
    private int total;
    /** 通过文件内校验的行（含 Excel 行号，供库内查重错误定位） */
    @Getter
    private final List<ParsedRow> rows = new ArrayList<>();
    @Getter
    private final List<LeadImportResultVO.RowError> errors = new ArrayList<>();
    /** 文件内已出现的手机号 / 邮箱（查重用） */
    @Getter
    private final Set<String> seenPhones = new HashSet<>();
    @Getter
    private final Set<String> seenEmails = new HashSet<>();

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        if (!REQUIRED_HEADER.equals(headMap.get(0))) {
            headerInvalid = true;
            // 提前终止解析；调用方根据 headerInvalid 标记转换为业务异常
            throw new LeadImportStopException();
        }
    }

    @Override
    public void invoke(LeadImportRow row, AnalysisContext context) {
        int rowIndex = context.readRowHolder().getRowIndex() + 1; // Excel 行号（从 1 开始）
        // EasyExcel 对空行也会回调，过滤整行为空的数据
        if (isBlankRow(row)) {
            return;
        }
        total++;

        List<String> rowErrors = new ArrayList<>();
        if (row.getName() == null || row.getName().isBlank()) {
            rowErrors.add("线索名称必填");
        } else if (row.getName().length() > 128) {
            rowErrors.add("线索名称超长");
        }
        if (StringUtils.hasText(row.getPhone()) && !seenPhones.add(row.getPhone())) {
            rowErrors.add("手机号在文件内重复: " + row.getPhone());
        }
        if (StringUtils.hasText(row.getEmail()) && !seenEmails.add(row.getEmail())) {
            rowErrors.add("邮箱在文件内重复: " + row.getEmail());
        }
        if (StringUtils.hasText(row.getSource()) && LeadSource.codeOfLabel(row.getSource()) == null) {
            rowErrors.add("来源不是有效字典值: " + row.getSource());
        }
        if (row.getContactName() != null && row.getContactName().length() > 64) {
            rowErrors.add("联系人姓名超长");
        }
        if (row.getRemark() != null && row.getRemark().length() > 500) {
            rowErrors.add("备注超长");
        }
        if (!rowErrors.isEmpty()) {
            errors.add(new LeadImportResultVO.RowError(rowIndex, String.join("; ", rowErrors)));
            return;
        }
        rows.add(new ParsedRow(rowIndex, row));
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        int rowIndex = context.readRowHolder().getRowIndex() + 1;
        // 单元格类型转换失败等解析异常：记录错误行，继续读后续行
        if (exception instanceof ExcelDataConvertException convert) {
            Object data = convert.getCellData() == null ? "" : convert.getCellData().toString();
            errors.add(new LeadImportResultVO.RowError(rowIndex, "单元格解析失败: " + data));
            return;
        }
        // 表头校验的终止标记向上抛，由调用方统一处理
        if (exception instanceof LeadImportStopException) {
            throw (LeadImportStopException) exception;
        }
        errors.add(new LeadImportResultVO.RowError(rowIndex, "行解析失败: " + exception.getMessage()));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 解析完成：结果由调用方从本监听器读取，无需处理
    }

    private boolean isBlankRow(LeadImportRow row) {
        return !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getContactName())
                && !StringUtils.hasText(row.getPhone())
                && !StringUtils.hasText(row.getEmail())
                && !StringUtils.hasText(row.getSource())
                && !StringUtils.hasText(row.getRemark());
    }

    /** 解析提前终止标记 */
    public static final class LeadImportStopException extends RuntimeException {
    }

    /**
     * 通过文件内校验的解析行。
     */
    public record ParsedRow(int rowIndex, LeadImportRow row) {
    }
}
