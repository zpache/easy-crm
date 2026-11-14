package cn.easykits.crm.modules.follow.strategy;

import cn.easykits.crm.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 客户活动时间更新策略：last_follow_time。
 */
@Component
@RequiredArgsConstructor
public class CustomerActivityUpdater implements FollowActivityUpdater {

    private final CustomerMapper customerMapper;

    @Override
    public String bizType() {
        return "customer";
    }

    @Override
    public void updateActivity(Long bizId) {
        customerMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<cn.easykits.crm.entity.Customer>lambdaUpdate()
                .eq(cn.easykits.crm.entity.Customer::getId, bizId)
                .set(cn.easykits.crm.entity.Customer::getLastFollowTime, LocalDateTime.now()));
    }
}
