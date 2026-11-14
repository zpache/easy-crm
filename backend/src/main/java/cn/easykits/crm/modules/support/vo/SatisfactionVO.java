package cn.easykits.crm.modules.support.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 满意度评价视图。
 */
@Data
public class SatisfactionVO {

    private Long id;

    private Long ticketId;

    private String ticketNo;

    private Integer score;

    private String content;

    private LocalDateTime evaluateTime;

    private Long ownerId;

    private String ownerName;

    private LocalDateTime createTime;
}
