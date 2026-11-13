package com.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。dataScope 与行级数据权限拦截器（common/datascope）对应，取用户全部角色中最大值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 角色编码，如 admin */
    private String code;

    /** 数据范围：1 本人 / 2 本部门 / 3 本部门及下级 / 4 全部 */
    private Integer dataScope;

    private Integer sort;

    /** 1 正常，0 禁用 */
    private Integer status;

    private String remark;
}
