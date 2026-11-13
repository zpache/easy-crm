package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：统一维护主键、审计字段、乐观锁版本号与逻辑删除标记。
 *
 * <p>对应表结构约定（建表时必须携带下列列）：
 * <pre>
 *   id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY   -- 自增主键
 *   create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP      -- 创建时间（自动填充）
 *   update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
 *                 ON UPDATE CURRENT_TIMESTAMP                          -- 更新时间（自动填充）
 *   create_by   BIGINT        NULL                                    -- 创建人 userId（自动填充）
 *   update_by   BIGINT        NULL                                    -- 修改人 userId（自动填充）
 *   version     INT           NOT NULL DEFAULT 0                      -- 乐观锁版本号
 *   deleted     TINYINT       NOT NULL DEFAULT 0                      -- 逻辑删除：0 未删 / 1 已删
 * </pre>
 * 字符集 utf8mb4，引擎 InnoDB。
 *
 * <p><b>主键选型说明（自增，非雪花）</b>：全库无特殊情况统一采用数据库自增主键。
 * CRM 为单体应用、单库写入，自增即可保证唯一；主键值域远小于 JS Number 安全整数
 * （2^53），前端无需字符串化；省去雪花算法的机器号分配与时钟回拨处理。
 * 如未来拆分多写库或需要跨库合并，再迁移到雪花 ID。
 */
@Data
public abstract class BaseEntity implements Serializable {

    /**
     * 自增主键。insert 后由数据库回填实体。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间，插入时由 MetaObjectHandler 自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入与更新时均由 MetaObjectHandler 自动填充 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 userId（登录态下的管理员）；无登录态的写入为 null */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /** 最后修改人 userId，每次 update 由 MetaObjectHandler 覆盖写入 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 乐观锁版本号。
     * updateById 时 MyBatis-Plus 会把它拼进 where 条件（version = ?）并自动 +1，
     * 受影响行数为 0 说明期间已被别人改过，业务层据此提示「数据已被修改」。
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    @TableField(value = "deleted", select = false)
    private Integer deleted;
}
