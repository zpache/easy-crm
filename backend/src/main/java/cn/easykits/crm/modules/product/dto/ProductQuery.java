package cn.easykits.crm.modules.product.dto;

import lombok.Data;

/**
 * 产品分页查询参数。
 */
@Data
public class ProductQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 名称/编码模糊匹配 */
    private String keyword;

    private String category;

    private Integer status;
}
