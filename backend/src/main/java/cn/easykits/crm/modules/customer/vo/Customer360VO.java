package cn.easykits.crm.modules.customer.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 客户 360 视图：基本信息 + 联系人 + 商机 + 跟进记录。
 * 商机按 customer_id、跟进记录按 biz_type='customer' AND biz_id 查询，均受行级数据权限约束。
 */
@Data
public class Customer360VO {

    /** 基本信息 */
    private CustomerVO customer;

    /** 联系人列表（主联系人排最前） */
    private List<Map<String, Object>> contacts;

    /** 商机列表 */
    private List<Map<String, Object>> opportunities;

    /** 跟进记录（最近若干条，按 create_time 倒序） */
    private List<Map<String, Object>> followRecords;
}
