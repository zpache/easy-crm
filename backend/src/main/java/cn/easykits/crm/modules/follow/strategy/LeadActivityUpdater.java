package cn.easykits.crm.modules.follow.strategy;

import cn.easykits.crm.mapper.LeadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 线索活动时间更新策略：last_follow_time。
 */
@Component
@RequiredArgsConstructor
public class LeadActivityUpdater implements FollowActivityUpdater {

    private final LeadMapper leadMapper;

    @Override
    public String bizType() {
        return "lead";
    }

    @Override
    public void updateActivity(Long bizId) {
        leadMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<cn.easykits.crm.entity.Lead>lambdaUpdate()
                .eq(cn.easykits.crm.entity.Lead::getId, bizId)
                .set(cn.easykits.crm.entity.Lead::getLastFollowTime, LocalDateTime.now()));
    }
}
