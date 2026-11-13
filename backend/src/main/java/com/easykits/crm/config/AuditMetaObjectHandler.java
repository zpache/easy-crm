package com.easykits.crm.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.easykits.crm.common.context.LoginUser;
import com.easykits.crm.common.context.LoginUserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：create_time / update_time / create_by / update_by / version。
 * <p>
 * create_by / update_by 取当前登录用户；登录接口这类还没有登录态的写入留空，
 * 由数据库 DEFAULT CURRENT_TIMESTAMP 兜底时间字段。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        // 版本号必须从 0 开始，交给 @Version 之后自增
        strictInsertFill(metaObject, "version", Integer.class, 0);

        Long userId = currentUserId();
        if (userId != null) {
            strictInsertFill(metaObject, "createBy", Long.class, userId);
            strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 注意：这里不能用 strictUpdateFill —— 实体是从库里查出来的，
        // 字段已有值，strict 模式会直接跳过，导致修改人一直是创建人。
        setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        Long userId = currentUserId();
        if (userId != null) {
            setFieldValByName("updateBy", userId, metaObject);
        }
    }

    /**
     * 当前登录用户 ID；无登录态（如登录接口本身、系统任务）返回 null。
     */
    private Long currentUserId() {
        LoginUser loginUser = LoginUserContext.get();
        return loginUser == null ? null : loginUser.userId();
    }
}
