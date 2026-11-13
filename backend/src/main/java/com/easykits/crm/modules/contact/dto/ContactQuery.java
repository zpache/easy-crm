package com.easykits.crm.modules.contact.dto;

import lombok.Data;

/**
 * 联系人分页查询参数（customerId 必传时按客户查）。
 */
@Data
public class ContactQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 所属客户 ID */
    private Long customerId;

    /** 姓名，模糊匹配 */
    private String name;

    /** 手机号，模糊匹配 */
    private String phone;
}
