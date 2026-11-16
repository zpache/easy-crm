package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典类型（dict_type 业务唯一，唯一性由 Service 层校验）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型编码（业务唯一，如 lead_source / customer_industry） */
    private String dictType;

    /** 字典名称 */
    private String name;

    private String remark;

    /** 1 内置字典（不允许删除，允许停用）；0 自定义 */
    private Integer builtIn;

    /** 1 启用，0 停用 */
    private Integer status;
}
