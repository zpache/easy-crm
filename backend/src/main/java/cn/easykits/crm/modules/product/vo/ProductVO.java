package cn.easykits.crm.modules.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品视图。
 */
@Data
public class ProductVO {

    private Long id;

    private String code;

    private String name;

    private String category;

    private String specification;

    private String unit;

    private Integer status;

    private String statusName;

    private BigDecimal costPrice;

    private BigDecimal standardPrice;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
