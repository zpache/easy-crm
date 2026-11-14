package cn.easykits.crm.modules.lead.task;

import cn.easykits.crm.common.datascope.DataScopeContext;
import cn.easykits.crm.modules.lead.service.LeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 公海自动回收定时任务。
 * <p>规则：状态为「跟进中」且最近一条跟进记录（crm_follow_record.create_time）
 * 超过 N 天的线索，自动回收至公海（owner_id 置空、状态回退为新建）；无跟进记录时
 * 回退 last_follow_time / create_time，恰好 N 天不回收。
 * <p><b>cron 配置位置</b>：application.yml → {@code easy-crm.lead.recycle-cron}
 * （默认每天凌晨 2 点执行）；回收天数配置在 {@code easy-crm.lead.recycle-days}（默认 15）。
 * <p>定时任务无登录上下文，必须显式声明系统内部调用，否则行级数据权限会 fail closed 拒绝扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeadRecycleTask {

    private final LeadService leadService;

    /** 无跟进天数阈值，超过即回收至公海 */
    @Value("${easy-crm.lead.recycle-days:15}")
    private int recycleDays;

    @Scheduled(cron = "${easy-crm.lead.recycle-cron:0 0 2 * * ?}")
    public void recycleIdleLeads() {
        DataScopeContext.runAsSystem(() -> {
            int count = leadService.recycleIdleToPool(recycleDays);
            if (count > 0) {
                log.info("公海自动回收完成：{} 条线索（无跟进超过 {} 天）", count, recycleDays);
            }
        });
    }
}
