package cn.easykits.crm.modules.support.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库视图。
 */
@Data
public class KnowledgeVO {

    private Long id;

    private String category;

    private String title;

    private String content;

    private String tags;

    private String publishStatus;

    private String publishStatusName;

    private Long viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
