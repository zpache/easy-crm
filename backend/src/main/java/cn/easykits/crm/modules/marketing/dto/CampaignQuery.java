package cn.easykits.crm.modules.marketing.dto;

import lombok.Data;

/**
 * 营销活动分页查询参数。
 * 行级数据权限由拦截器按登录用户自动注入 owner_id / dept_id 条件。
 */
@Data
public class CampaignQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 活动名称 / 编码，模糊匹配 */
    private String keyword;

    private Integer type;

    private Integer status;
}
