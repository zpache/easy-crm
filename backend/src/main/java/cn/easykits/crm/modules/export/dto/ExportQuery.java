package cn.easykits.crm.modules.export.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 导出 / 模板下载通用参数。
 * <p>日期仅按 create_time 过滤（各业务表均继承 BaseEntity 带 create_time）。
 */
@Data
public class ExportQuery {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** 可选状态过滤（仅对含 status 列的类型生效） */
    private Integer status;

    /** 名称/编码模糊匹配（对支持的类型生效） */
    private String keyword;

    /** 模板下载时的业务类型：leads/customers/contacts/opportunities/orders */
    private String type;
}
