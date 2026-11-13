package com.easykits.crm.modules.customer.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 跨模块创建客户的参数（线索转化使用）。
 * phone / email / address 本轮 crm_customer 无对应列，轮次 3 后扩展客户明细字段时落库（TODO）。
 */
@Data
public class CustomerInfo {

    @Size(max = 128, message = "客户名称不能超过 128 个字符")
    private String name;

    @Size(max = 32)
    private String phone;

    @Size(max = 128)
    private String email;

    @Size(max = 255)
    private String address;
}
