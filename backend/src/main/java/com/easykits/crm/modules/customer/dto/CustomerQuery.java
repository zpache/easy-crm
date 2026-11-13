package com.easykits.crm.modules.customer.dto;

import lombok.Data;

/**
 * 客户分页查询参数。行级数据权限由拦截器按登录用户注入 owner_id / dept_id 条件。
 */
@Data
public class CustomerQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 客户名称，模糊匹配 */
    private String name;

    /** 行业字典值 */
    private Integer industry;

    /** 状态：1 正常 2 公海 3 已合并（默认排除已合并，显式传 3 可查） */
    private Integer status;

    /** 负责人 ID；传 -1 查公海 */
    private Long ownerId;
}
