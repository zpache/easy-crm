package cn.easykits.crm.modules.opportunity.task;

import cn.easykits.crm.modules.opportunity.service.OpportunityServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商机停滞检测定时任务：扫描未终态商机，按 last_activity_at 计算
 * 停滞等级（7 天黄 / 14 天橙 / 30 天红，见 OpportunityServiceImpl.staleLevelOf），
 * 写入 crm_reminder（biz_type = opportunity_stale），同商机旧提醒先清后写。
 * <p><b>cron 配置位置</b>：application.yml → {@code easy-crm.opportunity.stale-cron}
 * （默认每天 03:00 执行）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityStaleTask {

    private final OpportunityServiceImpl opportunityService;

    @Scheduled(cron = "${easy-crm.opportunity.stale-cron:0 0 3 * * ?}")
    public void scanStale() {
        int count = opportunityService.scanStaleOpportunities();
        if (count > 0) {
            log.info("商机停滞扫描完成：{} 条提醒写入", count);
        }
    }
}
