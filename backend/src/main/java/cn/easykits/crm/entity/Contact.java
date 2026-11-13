package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联系人：挂在客户下，同一客户可标记一个主联系人（应用层 + 事务保证唯一）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contact")
public class Contact extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属客户 ID */
    private Long customerId;

    private String name;

    private String phone;

    private String email;

    /** 职位 */
    private String position;

    /** 1 主联系人（同客户唯一，应用层保证） */
    private Integer isPrimary;

    private String remark;
}
