package cn.easykits.crm.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品创建 / 更新表单（编码唯一；金额允许为 0，不允许负数）。
 */
@Data
public class ProductForm {

    @NotBlank(message = "产品编码不能为空")
    @Size(max = 64, message = "产品编码不能超过 64 个字符")
    private String code;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称不能超过 128 个字符")
    private String name;

    @Size(max = 64, message = "分类不能超过 64 个字符")
    private String category;

    @Size(max = 128, message = "规格不能超过 128 个字符")
    private String specification;

    @Size(max = 16, message = "单位不能超过 16 个字符")
    private String unit;

    /** 0 草稿 / 1 上架 / 2 停用 */
    private Integer status;

    private BigDecimal costPrice;

    private BigDecimal standardPrice;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
