package cn.easykits.crm.modules.approval.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.ApprovalInstance;
import cn.easykits.crm.mapper.ApprovalInstanceMapper;
import cn.easykits.crm.modules.approval.dto.ApprovalQuery;
import cn.easykits.crm.modules.approval.vo.ApprovalInstanceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批中心（只读）。
 *
 * <p>审批流对接到外部系统，本服务只提供三个入口列表：
 * 待办（审批中且当前审批人是我）、已办（已结束且我审批过）、已发起（我提交的）。
 * 不提供通过 / 驳回，动作在外部审批系统完成；页面通过 externalUrl 跳转。
 *
 * <p>注意：本表不登记行级数据权限（是「我的收件箱」语义），过滤条件直接按
 * 当前登录用户 ID 拼接在 LambdaQuery 中。
 */
@Service
@RequiredArgsConstructor
public class ApprovalInstanceServiceImpl extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance> {

    private static final Map<String, String> BIZ_TYPE_NAMES = Map.of(
            "QUOTE", "报价单",
            "CONTRACT", "合同",
            "ORDER", "订单",
            "INVOICE", "开票",
            "REFUND", "退款",
            "OTHER", "其他");

    private static final Map<Integer, String> STATUS_NAMES = Map.of(
            0, "审批中", 1, "已通过", 2, "已驳回", 3, "已撤销");

    public PageResult<ApprovalInstanceVO> pageQuery(ApprovalQuery query) {
        Long userId = LoginUserContext.userId();
        String box = StringUtils.hasText(query.getBox()) ? query.getBox() : "todo";
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;

        LambdaQueryWrapper<ApprovalInstance> wrapper = Wrappers.<ApprovalInstance>lambdaQuery()
                .and(keyword != null, w -> w.like(ApprovalInstance::getTitle, keyword)
                        .or().like(ApprovalInstance::getBizNo, keyword))
                .eq(StringUtils.hasText(query.getBizType()), ApprovalInstance::getBizType, query.getBizType())
                .eq(query.getStatus() != null, ApprovalInstance::getStatus, query.getStatus())
                .orderByDesc(ApprovalInstance::getApplyTime)
                .orderByDesc(ApprovalInstance::getId);

        switch (box) {
            case "todo" -> wrapper.eq(ApprovalInstance::getCurrentApproverId, userId)
                    .eq(ApprovalInstance::getStatus, 0);
            case "done" -> wrapper.eq(ApprovalInstance::getCurrentApproverId, userId)
                    .in(ApprovalInstance::getStatus, List.of(1, 2));
            case "applied" -> wrapper.eq(ApprovalInstance::getApplyUserId, userId);
            default -> throw new BizException("审批列表类型不合法: " + box + "（仅支持 todo / done / applied）");
        }

        Page<ApprovalInstance> page = page(Page.of(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page, this::toVO);
    }

    /** 三个入口的数量（用于页签角标）。 */
    public Map<String, Long> summary() {
        Long userId = LoginUserContext.userId();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("todo", count(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getCurrentApproverId, userId)
                .eq(ApprovalInstance::getStatus, 0)));
        result.put("done", count(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getCurrentApproverId, userId)
                .in(ApprovalInstance::getStatus, List.of(1, 2))));
        result.put("applied", count(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getApplyUserId, userId)));
        return result;
    }

    public ApprovalInstanceVO detail(Long id) {
        ApprovalInstance entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "审批实例不存在");
        }
        return toVO(entity);
    }

    private ApprovalInstanceVO toVO(ApprovalInstance entity) {
        ApprovalInstanceVO vo = new ApprovalInstanceVO();
        vo.setId(entity.getId());
        vo.setBizType(entity.getBizType());
        vo.setBizTypeName(BIZ_TYPE_NAMES.getOrDefault(entity.getBizType(), entity.getBizType()));
        vo.setBizId(entity.getBizId());
        vo.setBizNo(entity.getBizNo());
        vo.setTitle(entity.getTitle());
        vo.setApplyUserId(entity.getApplyUserId());
        vo.setApplyUserName(entity.getApplyUserName());
        vo.setCurrentApproverId(entity.getCurrentApproverId());
        vo.setCurrentApproverName(entity.getCurrentApproverName());
        vo.setNodeName(entity.getNodeName());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(STATUS_NAMES.getOrDefault(entity.getStatus(), "未知"));
        vo.setApplyTime(entity.getApplyTime());
        vo.setFinishTime(entity.getFinishTime());
        vo.setSourceSystem(entity.getSourceSystem());
        vo.setExternalUrl(entity.getExternalUrl());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
