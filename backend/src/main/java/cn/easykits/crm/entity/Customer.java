package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户：已转化的企业实体，商机 / 订单 / 回款的挂载点。
 * status：1 正常 / 2 公海 / 3 已合并 / 4 成交客户（merged_to_id 指向合并目标）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class Customer extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户名称 */
    private String name;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 行业字典值（CustomerDicts） */
    private Integer industry;

    /** 规模字典值 */
    private Integer scale;

    /** 区域字典值 */
    private Integer region;

    /** 客户级别字典值 */
    private Integer level;

    /** 1 正常 / 2 公海 / 3 已合并 / 4 成交客户 */
    private Integer status;

    /** 负责人，NULL 表示公海 */
    private Long ownerId;

    /** 归属部门 */
    private Long deptId;

    /** 合并目标客户 ID（status=3 时有效） */
    private Long mergedToId;

    /** 最近跟进时间（公海回收依据） */
    private LocalDateTime lastFollowTime;

    private String remark;
}
