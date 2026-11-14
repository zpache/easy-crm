package cn.easykits.crm.modules.support.task;

import cn.easykits.crm.common.datascope.DataScopeContext;
import cn.easykits.crm.modules.support.service.ServiceTicketServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA 超时升级定时任务：扫描超时工单，写 overdue_flag 与 crm_reminder。
 *
 * <p><b>系统上下文</b>：定时任务无登录态，而行级数据权限拦截器现在对无登录上下文
 * fail closed，因此执行体统一用
 * {@code DataScopeContext.runAsSystem(() -> { ... })} 包裹，使扫描期间的数据权限
 * 以系统身份（全部数据范围）放行。
 *
 * <p><b>cron 配置位置</b>：application.yml → {@code easy-crm.support.sla-cron}
 * （默认每天 04:00 执行，与商机停滞扫描 03:00 错峰）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceSlaTask {

    private final ServiceTicketServiceImpl serviceTicketService;

    @Scheduled(cron = "${easy-crm.support.sla-cron:0 0 4 * * ?}")
    public void scanOverdue() {
        DataScopeContext.runAsSystem(() -> {
            int count = serviceTicketService.scanOverdueTickets();
            if (count > 0) {
                log.info("服务工单 SLA 超时扫描完成：{} 条提醒写入", count);
            }
        });
    }
}
