package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 产品。金额字段统一 BigDecimal，对应 DECIMAL(15,2)。
 * status：0 草稿 / 1 上架 / 2 停用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_product")
public class Product extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产品编码，业务唯一 */
    private String code;

    private String name;

    private String category;

    private String specification;

    private String unit;

    private Integer status;

    private BigDecimal costPrice;

    private BigDecimal standardPrice;

    private String remark;
}
