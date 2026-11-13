package com.easykits.crm.modules.lead.dto;

import com.easykits.crm.modules.lead.constant.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线索更新表单。status 可选：提供时按状态机校验手工流转（只能前进；
 * 进入「已验证」只能走转化接口）。
 */
@Data
public class LeadUpdateForm {

    @Size(max = 128, message = "线索名称不能超过 128 个字符")
    private String name;

    @Size(max = 64, message = "联系人姓名不能超过 64 个字符")
    private String contactName;

    @Size(max = 32, message = "手机号不能超过 32 个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过 128 个字符")
    private String email;

    /** 来源字典值，见 {@link LeadSource#isValid(Integer)} */
    private Integer source;

    /** 可选状态流转：0→1、0→3、1→3；1→2 / 0→2 必须走转化接口 */
    private Integer status;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
