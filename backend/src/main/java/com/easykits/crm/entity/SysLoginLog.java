package com.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端登录日志：成功 / 失败记录（不继承 BaseEntity：日志表无乐观锁与逻辑删除）。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long userId;

    private String ip;

    private String userAgent;

    /** 1 成功 0 失败 */
    private Integer success;

    /** 说明：失败原因 */
    private String message;

    private LocalDateTime createdAt;
}
