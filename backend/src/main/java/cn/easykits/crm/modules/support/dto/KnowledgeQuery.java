package cn.easykits.crm.modules.support.dto;

import lombok.Data;

/**
 * 知识库检索参数。keyword 对标题 / 分类 / 标签 / 内容做模糊匹配（检索）。
 */
@Data
public class KnowledgeQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 检索关键字：标题 / 分类 / 标签 / 内容 */
    private String keyword;

    private String category;

    private String publishStatus;
}
