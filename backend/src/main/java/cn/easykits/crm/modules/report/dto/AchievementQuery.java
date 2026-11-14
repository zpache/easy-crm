package cn.easykits.crm.modules.report.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 达成分析查询参数：按时间（年月区间）+ 部门 + 负责人筛选。
 */
@Data
public class AchievementQuery {

    /** 起始年月 YYYY-MM（含） */
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "起始年月格式必须为 YYYY-MM")
    private String startMonth;

    /** 结束年月 YYYY-MM（含） */
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "结束年月格式必须为 YYYY-MM")
    private String endMonth;

    private Long deptId;

    private Long ownerId;
}
