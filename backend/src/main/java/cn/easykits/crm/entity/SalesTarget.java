package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售目标。targetMonth 格式 YYYY-MM；deptId / ownerId 至少一个有值。
 * 达成分析按「年月 + 部门 + 负责人」与商机/订单实绩比对。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_sales_target")
public class SalesTarget extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标年月，格式 YYYY-MM */
    private String targetMonth;

    /** NULL = 不限部门 */
    private Long deptId;

    /** NULL = 部门整体目标（非个人目标） */
    private Long ownerId;

    private BigDecimal targetAmount;

    private String remark;
}
