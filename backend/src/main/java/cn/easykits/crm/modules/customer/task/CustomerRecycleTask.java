package cn.easykits.crm.modules.customer.task;

import cn.easykits.crm.modules.customer.service.CustomerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 客户公海自动回收定时任务（与线索公海规则独立配置）。
 * <p>规则：状态「正常」且超过 N 天无跟进的客户，回收至公海（owner_id 置空、status 置公海）。
 * <p><b>cron 配置位置</b>：application.yml → {@code easy-crm.customer.recycle-cron}
 * （默认每天 02:30 执行，与线索回收错峰）；天数配置 {@code easy-crm.customer.recycle-days}（默认 30）。
 * <p>TODO（轮次 4 跟进记录表建立后）：回收依据从 last_follow_time 改为最近一条跟进记录时间。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerRecycleTask {

    private final CustomerServiceImpl customerService;

    /** 无跟进天数阈值，超过即回收至公海 */
    @Value("${easy-crm.customer.recycle-days:30}")
    private int recycleDays;

    @Scheduled(cron = "${easy-crm.customer.recycle-cron:0 30 2 * * ?}")
    public void recycleIdleCustomers() {
        int count = customerService.recycleIdleToPool(recycleDays);
        if (count > 0) {
            log.info("客户公海自动回收完成：{} 个客户（无跟进超过 {} 天）", count, recycleDays);
        }
    }
}
