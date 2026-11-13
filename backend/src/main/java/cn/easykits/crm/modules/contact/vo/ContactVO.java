package cn.easykits.crm.modules.contact.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人视图。
 */
@Data
public class ContactVO {

    private Long id;

    private Long customerId;

    private String customerName;

    private String name;

    private String phone;

    private String email;

    private String position;

    /** 是否主联系人 */
    private Boolean isPrimary;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
