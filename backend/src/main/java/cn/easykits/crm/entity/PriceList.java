package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 产品价目表：某产品在某生效区间内的价格。
 * effectiveDate 为空表示立即生效，expireDate 为空表示长期有效。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_price_list")
public class PriceList extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private BigDecimal price;

    private LocalDate effectiveDate;

    private LocalDate expireDate;

    /** 1 有效 / 0 失效 */
    private Integer status;

    private String remark;
}
