package cn.easykits.crm.modules.follow.strategy;

import cn.easykits.crm.mapper.OpportunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 商机活动时间更新策略：last_activity_at（停滞检测依据）。
 */
@Component
@RequiredArgsConstructor
public class OpportunityActivityUpdater implements FollowActivityUpdater {

    private final OpportunityMapper opportunityMapper;

    @Override
    public String bizType() {
        return "opportunity";
    }

    @Override
    public void updateActivity(Long bizId) {
        opportunityMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<cn.easykits.crm.entity.Opportunity>lambdaUpdate()
                .eq(cn.easykits.crm.entity.Opportunity::getId, bizId)
                .set(cn.easykits.crm.entity.Opportunity::getLastActivityAt, LocalDateTime.now()));
    }
}
