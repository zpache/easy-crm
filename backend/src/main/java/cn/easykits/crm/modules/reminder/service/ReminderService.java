package cn.easykits.crm.modules.reminder.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Reminder;
import cn.easykits.crm.mapper.ReminderMapper;
import cn.easykits.crm.modules.reminder.dto.ReminderQuery;
import cn.easykits.crm.modules.reminder.vo.ReminderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * 消息提醒服务：读取链路（列表 / 未读数 / 标记已读 / 全部已读）。
 * <p>crm_reminder 未登记行级数据权限（私有提醒），所有查询与写操作都在 Service 层
 * 显式按当前登录用户 ownerId 过滤，防止越权读取或标记他人提醒。
 * <p>写入由 OpportunityServiceImpl.scanStaleOpportunities 等停滞扫描任务完成，本服务不负责生产。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService extends ServiceImpl<ReminderMapper, Reminder> {

    /** 提醒等级标签（与停滞扫描写入的 1/2/3 保持一致） */
    private static final Map<Integer, String> LEVEL_NAMES = Map.of(
            1, "黄", 2, "橙", 3, "红");

    /**
     * 分页：固定按当前用户 ownerId 过滤，可选只查未读，按提醒时间倒序。
     */
    public PageResult<ReminderVO> pageQuery(ReminderQuery query) {
        LoginUser user = LoginUserContext.require();
        Page<Reminder> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Reminder>lambdaQuery()
                        .eq(Reminder::getOwnerId, user.userId())
                        .eq(Boolean.TRUE.equals(query.getOnlyUnread()), Reminder::getReadFlag, 0)
                        .orderByDesc(Reminder::getRemindTime)
                        .orderByDesc(Reminder::getId));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 当前用户未读条数（铃铛角标）。
     */
    public long unreadCount() {
        LoginUser user = LoginUserContext.require();
        return count(Wrappers.<Reminder>lambdaQuery()
                .eq(Reminder::getOwnerId, user.userId())
                .eq(Reminder::getReadFlag, 0));
    }

    /**
     * 单条标记已读：校验归属，避免标记他人提醒。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        LoginUser user = LoginUserContext.require();
        Reminder reminder = getById(id);
        if (reminder == null || !Objects.equals(reminder.getOwnerId(), user.userId())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "提醒不存在");
        }
        if (reminder.getReadFlag() != null && reminder.getReadFlag() == 1) {
            return;
        }
        reminder.setReadFlag(1);
        if (!updateById(reminder)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 全部标记已读：仅作用于当前用户的未读提醒，返回本次置为已读的条数。
     */
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead() {
        LoginUser user = LoginUserContext.require();
        return baseMapper.update(null, Wrappers.<Reminder>lambdaUpdate()
                .eq(Reminder::getOwnerId, user.userId())
                .eq(Reminder::getReadFlag, 0)
                .set(Reminder::getReadFlag, 1));
    }

    private ReminderVO toVO(Reminder reminder) {
        ReminderVO vo = new ReminderVO();
        vo.setId(reminder.getId());
        vo.setBizType(reminder.getBizType());
        vo.setBizId(reminder.getBizId());
        vo.setOwnerId(reminder.getOwnerId());
        vo.setRemindLevel(reminder.getRemindLevel());
        vo.setLevelName(reminder.getRemindLevel() == null
                ? null : LEVEL_NAMES.get(reminder.getRemindLevel()));
        vo.setRemindTime(reminder.getRemindTime());
        vo.setReadFlag(reminder.getReadFlag());
        vo.setContent(reminder.getContent());
        vo.setCreateTime(reminder.getCreateTime());
        return vo;
    }
}
