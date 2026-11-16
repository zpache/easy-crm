package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典项（dict_type + value 业务唯一，唯一性由 Service 层校验）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典 ID（冗余列，归属以 dictType 为准） */
    private Long dictId;

    /** 字典类型编码（冗余自 sys_dict，避免联查） */
    private String dictType;

    /** 显示名称（Excel 导入按此匹配） */
    private String label;

    /** 存储值（数字码或英文码） */
    private String value;

    /** 排序，越小越前 */
    private Integer sortNo;

    /** 1 启用，0 停用 */
    private Integer status;

    private String remark;
}
