package cn.easykits.crm.modules.export.service;

import cn.easykits.crm.entity.Contact;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.entity.Lead;
import cn.easykits.crm.entity.Opportunity;
import cn.easykits.crm.entity.Order;
import cn.easykits.crm.mapper.ContactMapper;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.mapper.LeadMapper;
import cn.easykits.crm.mapper.OpportunityMapper;
import cn.easykits.crm.mapper.OrderMapper;
import cn.easykits.crm.modules.export.dto.ExportQuery;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CRM 流式导出服务（EasyExcel 4.0.3）。
 * <p>分页读取、分页写入：每页 500 行写一次 ExcelWriter，不把全表数据一次性装进内存，
 * 全程走 MyBatis-Plus LambdaQueryWrapper（无自定义 SQL、无字符串拼接）。
 * <p>导出端点设计为 POST，使现有 OperLogAspect（只拦截 POST/PUT/DELETE）自动记录写操作日志，
 * 无需业务代码手写日志。
 */
@Service
@RequiredArgsConstructor
public class CrmExportServiceImpl {

    private static final int PAGE_SIZE = 500;

    private final LeadMapper leadMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final OpportunityMapper opportunityMapper;
    private final OrderMapper orderMapper;

    /** 流式导出指定类型。 */
    public void stream(String type, ExportQuery query, HttpServletResponse response) throws IOException {
        prepareResponse(response, type);
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream())
                .head(headsOf(type)).build()) {
            WriteSheet sheet = EasyExcel.writerSheet(0, sheetNameOf(type)).build();
            long pageNum = 1;
            while (true) {
                List<List<Object>> rows = fetch(type, query, pageNum);
                if (rows.isEmpty()) {
                    break;
                }
                writer.write(rows, sheet);
                if (rows.size() < PAGE_SIZE) {
                    break;
                }
                pageNum++;
            }
        }
    }

    /** 模板下载：只写表头，不写数据行。 */
    public void template(String type, HttpServletResponse response) throws IOException {
        prepareResponse(response, "template-" + type);
        EasyExcel.write(response.getOutputStream())
                .head(headsOf(type))
                .sheet(sheetNameOf(type))
                .doWrite(List.of());
    }

    private void prepareResponse(HttpServletResponse response, String baseName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(
                baseName + "_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.setHeader("Cache-Control", "no-store, no-cache");
    }

    private List<List<Object>> fetch(String type, ExportQuery query, long pageNum) {
        return switch (type) {
            case "leads" -> fetchLeads(query, pageNum);
            case "customers" -> fetchCustomers(query, pageNum);
            case "contacts" -> fetchContacts(query, pageNum);
            case "opportunities" -> fetchOpportunities(query, pageNum);
            case "orders" -> fetchOrders(query, pageNum);
            default -> List.of();
        };
    }

    private List<List<Object>> fetchLeads(ExportQuery q, long pageNum) {
        Page<Lead> page = leadMapper.selectPage(Page.of(pageNum, PAGE_SIZE),
                Wrappers.<Lead>lambdaQuery()
                        .ge(startOf(q) != null, Lead::getCreateTime, startOf(q))
                        .le(endOf(q) != null, Lead::getCreateTime, endOf(q))
                        .eq(q.getStatus() != null, Lead::getStatus, q.getStatus())
                        .like(StringUtils.hasText(q.getKeyword()), Lead::getName, q.getKeyword())
                        .orderByDesc(Lead::getId));
        return page.getRecords().stream()
                .map(e -> row(e.getId(), e.getName(), e.getContactName(), e.getPhone(), e.getEmail(),
                        e.getSource(), e.getStatus(), e.getOwnerId(), e.getLastFollowTime(), e.getCreateTime()))
                .collect(Collectors.toList());
    }

    private List<List<Object>> fetchCustomers(ExportQuery q, long pageNum) {
        Page<Customer> page = customerMapper.selectPage(Page.of(pageNum, PAGE_SIZE),
                Wrappers.<Customer>lambdaQuery()
                        .ge(startOf(q) != null, Customer::getCreateTime, startOf(q))
                        .le(endOf(q) != null, Customer::getCreateTime, endOf(q))
                        .eq(q.getStatus() != null, Customer::getStatus, q.getStatus())
                        .like(StringUtils.hasText(q.getKeyword()), Customer::getName, q.getKeyword())
                        .orderByDesc(Customer::getId));
        return page.getRecords().stream()
                .map(e -> row(e.getId(), e.getName(), e.getCreditCode(), e.getIndustry(), e.getLevel(),
                        e.getStatus(), e.getOwnerId(), e.getDeptId(), e.getCreateTime()))
                .collect(Collectors.toList());
    }

    private List<List<Object>> fetchContacts(ExportQuery q, long pageNum) {
        Page<Contact> page = contactMapper.selectPage(Page.of(pageNum, PAGE_SIZE),
                Wrappers.<Contact>lambdaQuery()
                        .ge(startOf(q) != null, Contact::getCreateTime, startOf(q))
                        .le(endOf(q) != null, Contact::getCreateTime, endOf(q))
                        .like(StringUtils.hasText(q.getKeyword()), Contact::getName, q.getKeyword())
                        .orderByDesc(Contact::getId));
        return page.getRecords().stream()
                .map(e -> row(e.getId(), e.getCustomerId(), e.getName(), e.getPhone(), e.getEmail(),
                        e.getPosition(), e.getIsPrimary(), e.getCreateTime()))
                .collect(Collectors.toList());
    }

    private List<List<Object>> fetchOpportunities(ExportQuery q, long pageNum) {
        Page<Opportunity> page = opportunityMapper.selectPage(Page.of(pageNum, PAGE_SIZE),
                Wrappers.<Opportunity>lambdaQuery()
                        .ge(startOf(q) != null, Opportunity::getCreateTime, startOf(q))
                        .le(endOf(q) != null, Opportunity::getCreateTime, endOf(q))
                        .like(StringUtils.hasText(q.getKeyword()), Opportunity::getName, q.getKeyword())
                        .orderByDesc(Opportunity::getId));
        return page.getRecords().stream()
                .map(e -> row(e.getId(), e.getName(), e.getCustomerId(), e.getStageCode(), e.getAmount(),
                        e.getWinRate(), e.getExpectedCloseDate(), e.getActualAmount(), e.getOwnerId(),
                        e.getCreateTime()))
                .collect(Collectors.toList());
    }

    private List<List<Object>> fetchOrders(ExportQuery q, long pageNum) {
        Page<Order> page = orderMapper.selectPage(Page.of(pageNum, PAGE_SIZE),
                Wrappers.<Order>lambdaQuery()
                        .ge(startOf(q) != null, Order::getCreateTime, startOf(q))
                        .le(endOf(q) != null, Order::getCreateTime, endOf(q))
                        .eq(q.getStatus() != null, Order::getStatus, q.getStatus())
                        .like(StringUtils.hasText(q.getKeyword()), Order::getOrderNo, q.getKeyword())
                        .orderByDesc(Order::getId));
        return page.getRecords().stream()
                .map(e -> row(e.getId(), e.getOrderNo(), e.getCustomerId(), e.getTotalAmount(),
                        e.getStatus(), e.getShipDate(), e.getDeliveryDate(), e.getOwnerId(), e.getCreateTime()))
                .collect(Collectors.toList());
    }

    private LocalDateTime startOf(ExportQuery q) {
        return q.getStartDate() == null ? null : q.getStartDate().atStartOfDay();
    }

    private LocalDateTime endOf(ExportQuery q) {
        return q.getEndDate() == null ? null : q.getEndDate().atTime(LocalTime.MAX);
    }

    /** Arrays.asList 允许 null 元素（List.of 不允许），避免导出含空字段的行时 NPE。 */
    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private List<List<String>> headsOf(String type) {
        return switch (type) {
            case "leads" -> heads("ID", "线索名称", "联系人", "手机号", "邮箱", "来源", "状态",
                    "负责人ID", "最近跟进时间", "创建时间");
            case "customers" -> heads("ID", "客户名称", "信用代码", "行业", "级别", "状态",
                    "负责人ID", "部门ID", "创建时间");
            case "contacts" -> heads("ID", "客户ID", "姓名", "手机号", "邮箱", "职位", "主联系人", "创建时间");
            case "opportunities" -> heads("ID", "商机名称", "客户ID", "阶段编码", "预计金额", "赢单率",
                    "预计成交日期", "实际成交金额", "负责人ID", "创建时间");
            case "orders" -> heads("ID", "订单编号", "客户ID", "总金额", "状态", "发货日期",
                    "交付日期", "负责人ID", "创建时间");
            default -> heads("ID");
        };
    }

    private List<List<String>> heads(String... names) {
        return Arrays.stream(names).map(n -> List.of(n)).collect(Collectors.toList());
    }

    private String sheetNameOf(String type) {
        return switch (type) {
            case "leads" -> "线索";
            case "customers" -> "客户";
            case "contacts" -> "联系人";
            case "opportunities" -> "商机";
            case "orders" -> "订单";
            default -> "Sheet1";
        };
    }
}
