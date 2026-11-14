package cn.easykits.crm.modules.support.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Knowledge;
import cn.easykits.crm.mapper.KnowledgeMapper;
import cn.easykits.crm.modules.support.constant.SupportDicts;
import cn.easykits.crm.modules.support.dto.KnowledgeForm;
import cn.easykits.crm.modules.support.dto.KnowledgeQuery;
import cn.easykits.crm.modules.support.vo.KnowledgeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识库服务：CRUD + 检索（标题 / 分类 / 标签 / 内容模糊匹配）+ 浏览量递增。
 * crm_knowledge 为全局知识内容，不接行级数据权限。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> {

    /** 分页检索：keyword 对标题 / 分类 / 标签 / 内容做 OR 模糊匹配。 */
    public PageResult<KnowledgeVO> pageQuery(KnowledgeQuery query) {
        Page<Knowledge> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Knowledge>lambdaQuery()
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(Knowledge::getTitle, query.getKeyword())
                                .or().like(Knowledge::getCategory, query.getKeyword())
                                .or().like(Knowledge::getTags, query.getKeyword())
                                .or().like(Knowledge::getContent, query.getKeyword()))
                        .eq(StringUtils.hasText(query.getCategory()), Knowledge::getCategory, query.getCategory())
                        .eq(StringUtils.hasText(query.getPublishStatus()),
                                Knowledge::getPublishStatus, query.getPublishStatus())
                        .orderByDesc(Knowledge::getId));
        List<KnowledgeVO> vos = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(vos, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 详情（纯读，不递增浏览量）。 */
    public KnowledgeVO detail(Long id) {
        Knowledge knowledge = getById(id);
        if (knowledge == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "知识条目不存在");
        }
        return toVO(knowledge);
    }

    /** 浏览量 +1（独立接口，避免 GET 带写副作用）。 */
    @Transactional(rollbackFor = Exception.class)
    public void view(Long id) {
        Knowledge knowledge = getById(id);
        if (knowledge == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "知识条目不存在");
        }
        long current = knowledge.getViewCount() == null ? 0L : knowledge.getViewCount();
        baseMapper.update(null, Wrappers.<Knowledge>lambdaUpdate()
                .eq(Knowledge::getId, id)
                .set(Knowledge::getViewCount, current + 1));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(KnowledgeForm form) {
        validateDict(form);
        Knowledge knowledge = new Knowledge();
        applyForm(knowledge, form);
        knowledge.setViewCount(0L);
        save(knowledge);
        return knowledge.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, KnowledgeForm form) {
        Knowledge knowledge = getById(id);
        if (knowledge == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "知识条目不存在");
        }
        validateDict(form);
        applyForm(knowledge, form);
        if (!updateById(knowledge)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Knowledge knowledge = getById(id);
        if (knowledge == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "知识条目不存在");
        }
        removeById(id);
    }

    private void validateDict(KnowledgeForm form) {
        if (!SupportDicts.isValidKnowledgeStatus(form.getPublishStatus())) {
            throw new BizException("发布状态不合法: " + form.getPublishStatus());
        }
    }

    private void applyForm(Knowledge knowledge, KnowledgeForm form) {
        knowledge.setCategory(form.getCategory());
        knowledge.setTitle(form.getTitle());
        knowledge.setContent(form.getContent());
        knowledge.setTags(form.getTags());
        knowledge.setPublishStatus(form.getPublishStatus());
    }

    private KnowledgeVO toVO(Knowledge knowledge) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(knowledge.getId());
        vo.setCategory(knowledge.getCategory());
        vo.setTitle(knowledge.getTitle());
        vo.setContent(knowledge.getContent());
        vo.setTags(knowledge.getTags());
        vo.setPublishStatus(knowledge.getPublishStatus());
        vo.setPublishStatusName(SupportDicts.knowledgeStatusLabel(knowledge.getPublishStatus()));
        vo.setViewCount(knowledge.getViewCount());
        vo.setCreateTime(knowledge.getCreateTime());
        vo.setUpdateTime(knowledge.getUpdateTime());
        return vo;
    }
}
