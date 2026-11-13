package com.easykits.crm.modules.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 跨模块创建联系人的参数（线索转化使用）。
 */
@Data
public class ContactInfo {

    @Size(max = 64, message = "联系人姓名不能超过 64 个字符")
    private String name;

    @Size(max = 32)
    private String phone;

    @Email
    private String email;

    @Size(max = 64)
    private String position;
}
