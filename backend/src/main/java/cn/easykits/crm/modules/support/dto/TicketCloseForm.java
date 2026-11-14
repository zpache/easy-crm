package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单关闭表单。任意非终态可关闭，关闭写入 CLOSED 流转日志并置实际解决时间。
 */
@Data
public class TicketCloseForm {

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
