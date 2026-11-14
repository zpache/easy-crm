package cn.easykits.crm.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库新增 / 编辑表单。
 */
@Data
public class KnowledgeForm {

    @NotBlank(message = "分类不能为空")
    @Size(max = 64, message = "分类不能超过 64 个字符")
    private String category;

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题不能超过 128 个字符")
    private String title;

    private String content;

    @Size(max = 255, message = "标签不能超过 255 个字符")
    private String tags;

    /** 发布状态：DRAFT / PUBLISHED / OFFLINE */
    @NotBlank(message = "发布状态不能为空")
    private String publishStatus;
}
