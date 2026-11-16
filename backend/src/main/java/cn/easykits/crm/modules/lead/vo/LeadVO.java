package cn.easykits.crm.modules.lead.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索列表 / 详情视图（列表与详情共用，详情多返回转化 ID 等字段）。
 */
@Data
public class LeadVO {

    private Long id;

    /** 线索名称 / 公司名 */
    private String name;

    private String contactName;

    private String phone;

    private String email;

    /** 来源字典值 */
    private Integer source;

    /** 来源中文名 */
    private String sourceName;

    /** 来源营销活动 ID */
    private Long campaignId;

    /** 来源营销活动名称 */
    private String campaignName;

    /** 状态：0 新建 1 跟进中 2 已验证 3 不合格 */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    private Long ownerId;

    /** 负责人姓名（公海为 null） */
    private String ownerName;

    private Long deptId;

    private String deptName;

    private String remark;

    /** 转化生成的客户 ID（仅详情返回） */
    private Long convertedCustomerId;

    /** 转化生成的联系人 ID（仅详情返回） */
    private Long convertedContactId;

    /** 转化生成的商机 ID（仅详情返回） */
    private Long convertedOpportunityId;

    private LocalDateTime lastFollowTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer version;

    /** 公司名称是否与其他线索重复（true 时 duplicates 给出重复线索，仅提示不阻断） */
    private Boolean companyDuplicate;

    /** 与当前线索同名的其他线索（id + name），最多 5 条 */
    private List<DuplicateItem> duplicates;

    /**
     * 同名线索摘要。
     */
    public record DuplicateItem(Long id, String name) {
    }
}
