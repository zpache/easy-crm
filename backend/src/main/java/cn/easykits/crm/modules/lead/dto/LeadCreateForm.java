package cn.easykits.crm.modules.lead.dto;

import cn.easykits.crm.modules.lead.constant.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线索创建表单。
 */
@Data
public class LeadCreateForm {

    @NotBlank(message = "线索名称不能为空")
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

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
