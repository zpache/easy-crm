package cn.easykits.crm.modules.lead.dto;

import lombok.Data;

/**
 * 线索分页查询参数。
 * 行级数据权限由拦截器按登录用户自动注入 owner_id / dept_id 条件。
 */
@Data
public class LeadQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 线索名称 / 公司名，模糊匹配 */
    private String name;

    /** 手机号，模糊匹配 */
    private String phone;

    /** 状态：0 新建 1 跟进中 2 已验证 3 不合格 */
    private Integer status;

    /** 负责人 ID；传 -1 查公海（owner_id IS NULL） */
    private Long ownerId;

    /** 来源字典值 */
    private Integer source;
}
