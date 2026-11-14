package cn.easykits.crm.modules.export.controller;

import cn.easykits.crm.modules.export.dto.ExportQuery;
import cn.easykits.crm.modules.export.service.CrmExportServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * CRM 导出中心（EasyExcel 流式导出 + 模板下载）。
 * <p>端点统一使用 POST：现有 OperLogAspect 仅拦截 POST/PUT/DELETE 的写操作并自动落
 * sys_oper_log，因此无需在业务代码里手写操作日志。
 * <p>响应为二进制流（Content-Disposition: attachment），故方法返回 void 而非 Result——
 * 这是文件下载端点的必要例外，其余接口仍统一返回 Result。
 */
@Tag(name = "导出中心")
@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
public class CrmExportController {

    private final CrmExportServiceImpl exportService;

    @Operation(summary = "流式导出线索")
    @PostMapping("/leads")
    @PreAuthorize("hasAuthority('crm:export:lead')")
    public void exportLeads(@ModelAttribute ExportQuery query, HttpServletResponse response)
            throws IOException {
        exportService.stream("leads", query, response);
    }

    @Operation(summary = "流式导出客户")
    @PostMapping("/customers")
    @PreAuthorize("hasAuthority('crm:export:customer')")
    public void exportCustomers(@ModelAttribute ExportQuery query, HttpServletResponse response)
            throws IOException {
        exportService.stream("customers", query, response);
    }

    @Operation(summary = "流式导出联系人")
    @PostMapping("/contacts")
    @PreAuthorize("hasAuthority('crm:export:contact')")
    public void exportContacts(@ModelAttribute ExportQuery query, HttpServletResponse response)
            throws IOException {
        exportService.stream("contacts", query, response);
    }

    @Operation(summary = "流式导出商机")
    @PostMapping("/opportunities")
    @PreAuthorize("hasAuthority('crm:export:opportunity')")
    public void exportOpportunities(@ModelAttribute ExportQuery query, HttpServletResponse response)
            throws IOException {
        exportService.stream("opportunities", query, response);
    }

    @Operation(summary = "流式导出订单")
    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('crm:export:order')")
    public void exportOrders(@ModelAttribute ExportQuery query, HttpServletResponse response)
            throws IOException {
        exportService.stream("orders", query, response);
    }

    @Operation(summary = "下载导入模板（type=leads/customers/contacts/opportunities/orders）")
    @PostMapping("/template")
    @PreAuthorize("hasAuthority('crm:export:template')")
    public void template(@RequestParam(defaultValue = "leads") String type,
                         HttpServletResponse response) throws IOException {
        exportService.template(type, response);
    }
}
