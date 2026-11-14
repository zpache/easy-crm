package cn.easykits.crm.modules.follow.strategy;

/**
 * 跟进记录活动时间更新策略：按 biz_type 分发到对应业务对象，
 * 避免在服务里堆叠 if-else。新增业务类型只需新增一个实现类。
 */
public interface FollowActivityUpdater {

    /**
     * 该更新器负责的业务类型（follow biz_type 取值）。
     */
    String bizType();

    /**
     * 更新业务对象的活动时间字段（线索/客户 last_follow_time，商机 last_activity_at）。
     */
    void updateActivity(Long bizId);
}
