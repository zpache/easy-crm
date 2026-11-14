package cn.easykits.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库条目。publish_status 落 DRAFT / PUBLISHED / OFFLINE；view_count 通过
 * {@code POST /knowledge/{id}/view} 递增。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_knowledge")
public class Knowledge extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类 */
    private String category;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 标签（逗号分隔） */
    private String tags;

    /** 发布状态：DRAFT / PUBLISHED / OFFLINE */
    private String publishStatus;

    /** 浏览量 */
    private Long viewCount;
}
