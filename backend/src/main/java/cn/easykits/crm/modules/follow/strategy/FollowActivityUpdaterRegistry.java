package cn.easykits.crm.modules.follow.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 策略注册器：Spring 自动收集全部 {@link FollowActivityUpdater} 实现，
 * 按 biz_type 分发。未注册的 bizType（如 order，轮次 5）静默跳过。
 */
@Component
public class FollowActivityUpdaterRegistry {

    private final Map<String, FollowActivityUpdater> updaters;

    public FollowActivityUpdaterRegistry(List<FollowActivityUpdater> updaterList) {
        this.updaters = updaterList.stream()
                .collect(Collectors.toMap(FollowActivityUpdater::bizType, Function.identity()));
    }

    /**
     * 更新业务对象活动时间；无对应策略时不做任何事。
     */
    public void updateActivity(String bizType, Long bizId) {
        FollowActivityUpdater updater = updaters.get(bizType);
        if (updater != null) {
            updater.updateActivity(bizId);
        }
    }
}
