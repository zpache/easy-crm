package cn.easykits.crm.modules.marketing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 营销活动创建 / 更新表单。
 * <p>code 留空时由服务端生成 MC-yyyyMMddHHmmss-xxxx。
 */
@Data
public class CampaignForm {

    @Size(max = 64, message = "活动编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 128, message = "活动名称不能超过 128 个字符")
    private String name;

    /** 1 线上推广 / 2 线下活动 / 3 电话营销 / 4 渠道合作 / 5 内容营销 */
    @Min(value = 1, message = "活动类型不合法")
    @Max(value = 5, message = "活动类型不合法")
    private Integer type;

    @Size(max = 64, message = "渠道不能超过 64 个字符")
    private String channel;

    /** 0 草稿 / 1 进行中 / 2 已结束 / 3 已暂停 */
    @Min(value = 0, message = "状态不合法")
    @Max(value = 3, message = "状态不合法")
    private Integer status;

    @DecimalMin(value = "0.00", message = "预算不能为负数")
    private BigDecimal budget;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 0, message = "目标线索数不能为负数")
    private Integer targetLeads;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
