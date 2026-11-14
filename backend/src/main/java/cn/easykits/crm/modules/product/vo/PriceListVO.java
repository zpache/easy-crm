package cn.easykits.crm.modules.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 价目表视图（productName 由产品主数据回填）。
 */
@Data
public class PriceListVO {

    private Long id;

    private Long productId;

    private String productName;

    private BigDecimal price;

    private LocalDate effectiveDate;

    private LocalDate expireDate;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
