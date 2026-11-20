package cn.easykits.crm.modules.opportunity.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 商机事件监听器：统一在事务提交后（AFTER_COMMIT）触发，避免事务未提交就被下游消费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityEventListener {

    /**
     * 赢单事件：自动建单由 modules/order/listener/OpportunityWonOrderListener 负责
     * （同事件独立监听，带系统豁免）；本监听器仅保留审计日志。
     */

    /**
     * 阶段变更事件：阶段历史已由 changeStage 事务内写入 crm_opportunity_stage_log
     * （时间线聚合的数据源），本监听器仅保留审计日志。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStageChanged(OpportunityStageChangedEvent event) {
        log.info("[商机阶段变更] opportunityId={}, name={}, {} -> {}",
                event.opportunityId(), event.name(),
                event.fromStageCode(), event.toStageCode());
    }
}
