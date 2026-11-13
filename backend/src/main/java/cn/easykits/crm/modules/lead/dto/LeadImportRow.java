package cn.easykits.crm.modules.lead.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 线索 Excel 导入行（EasyExcel 注解映射）。
 * 列顺序即模板列顺序：线索名称 | 联系人 | 手机号 | 邮箱 | 来源 | 备注。
 */
@Data
public class LeadImportRow {

    @ExcelProperty(value = "线索名称", index = 0)
    private String name;

    @ExcelProperty(value = "联系人", index = 1)
    private String contactName;

    @ExcelProperty(value = "手机号", index = 2)
    private String phone;

    @ExcelProperty(value = "邮箱", index = 3)
    private String email;

    /** 中文标签（官网 / 电话 / 广告投放 / 客户转介绍 / 展会活动 / 其他），导入时按标签匹配字典码 */
    @ExcelProperty(value = "来源", index = 4)
    private String source;

    @ExcelProperty(value = "备注", index = 5)
    private String remark;
}
