package cn.easykits.crm.modules.lead.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 线索转化请求。
 * <p>商机必须创建（由后端保证）；客户、联系人可「创建」或「关联已有」，都不选则对应 ID 为空。
 * createCustomer=true 时 customerInfo.name 必填；false 且 customerId 非空则为关联已有。
 * 联系人同理；两者都未提供时 converted_contact_id 落空。
 */
@Data
public class ConvertForm {

    /** 是否创建新客户 */
    private Boolean createCustomer;

    /** 关联已有客户 ID（createCustomer 非 true 时生效） */
    private Long customerId;

    @Valid
    private CustomerInfo customerInfo;

    /** 是否创建新联系人 */
    private Boolean createContact;

    /** 关联已有联系人 ID（createContact 非 true 时生效） */
    private Long contactId;

    @Valid
    private ContactInfo contactInfo;

    /** 商机名称，缺省取线索名称 */
    @Size(max = 128, message = "商机名称不能超过 128 个字符")
    private String opportunityName;

    /**
     * 新建客户信息。
     */
    @Data
    public static class CustomerInfo {

        @Size(max = 128, message = "客户名称不能超过 128 个字符")
        private String name;

        @Size(max = 32, message = "手机号不能超过 32 个字符")
        private String phone;

        @Email(message = "客户邮箱格式不正确")
        private String email;

        @Size(max = 255, message = "地址不能超过 255 个字符")
        private String address;
    }

    /**
     * 新建联系人信息。
     */
    @Data
    public static class ContactInfo {

        @Size(max = 64, message = "联系人姓名不能超过 64 个字符")
        private String name;

        @Size(max = 32, message = "手机号不能超过 32 个字符")
        private String phone;

        @Email(message = "联系人邮箱格式不正确")
        private String email;

        @Size(max = 64, message = "职位不能超过 64 个字符")
        private String position;
    }
}
