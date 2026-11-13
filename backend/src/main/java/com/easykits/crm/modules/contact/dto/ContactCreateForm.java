package com.easykits.crm.modules.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 联系人创建 / 编辑表单。编辑时 customerId / isPrimary 以原记录为准，
 * isPrimary=true 时同客户其余主联系人自动取消。
 */
@Data
public class ContactCreateForm {

    /** 所属客户（仅创建时使用） */
    @NotNull(message = "所属客户不能为空")
    private Long customerId;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 64, message = "联系人姓名不能超过 64 个字符")
    private String name;

    @Size(max = 32, message = "手机号不能超过 32 个字符")
    private String phone;

    @Size(max = 128, message = "邮箱不能超过 128 个字符")
    private String email;

    @Size(max = 64, message = "职位不能超过 64 个字符")
    private String position;

    /** 是否主联系人（同客户唯一） */
    private Boolean isPrimary;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
