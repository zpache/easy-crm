package cn.easykits.crm.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价目表创建 / 更新表单。
 */
@Data
public class PriceListForm {

    @NotNull(message = "产品不能为空")
    private Long productId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    private BigDecimal price;

    /** 为空表示立即生效 */
    private LocalDate effectiveDate;

    /** 为空表示长期有效 */
    private LocalDate expireDate;

    /** 1 有效 / 0 失效 */
    private Integer status;

    private String remark;
}
