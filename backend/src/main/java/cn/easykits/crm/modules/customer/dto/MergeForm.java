package cn.easykits.crm.modules.customer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户合并请求：source 合并到 target。
 */
@Data
public class MergeForm {

    @NotNull(message = "被合并客户 ID 不能为空")
    private Long sourceId;

    @NotNull(message = "合并目标客户 ID 不能为空")
    private Long targetId;
}
