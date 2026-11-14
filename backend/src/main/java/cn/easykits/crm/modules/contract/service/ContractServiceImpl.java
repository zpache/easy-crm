package cn.easykits.crm.modules.contract.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.context.LoginUser;
import cn.easykits.crm.common.context.LoginUserContext;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Contract;
import cn.easykits.crm.entity.Customer;
import cn.easykits.crm.mapper.ContractMapper;
import cn.easykits.crm.mapper.CustomerMapper;
import cn.easykits.crm.modules.contract.dto.ContractForm;
import cn.easykits.crm.modules.contract.dto.ContractQuery;
import cn.easykits.crm.modules.contract.vo.ContractVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 合同服务：编号唯一、金额非负、CRUD / 分页 / 状态流转。
 */
@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CustomerMapper customerMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(ContractForm form) {
        String contractNo = resolveNo(form.getContractNo(), null);
        LoginUser user = LoginUserContext.require();
        Contract entity = new Contract();
        applyForm(entity, form);
        entity.setContractNo(contractNo);
        entity.setStatus(form.getStatus() == null ? 0 : form.getStatus());
        entity.setOwnerId(user.userId());
        entity.setDeptId(user.deptId());
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, ContractForm form) {
        Contract entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        applyForm(entity, form);
        entity.setContractNo(resolveNo(form.getContractNo(), id));
        entity.setStatus(form.getStatus() == null ? entity.getStatus() : form.getStatus());
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /** 状态流转：终态（已归档/已作废）不可再变更。 */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer target) {
        Contract entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        if (entity.getStatus() != null && (entity.getStatus() == 2 || entity.getStatus() == 3)) {
            throw new BizException("合同已处于终态，不可再变更状态");
        }
        if (target == null || target < 0 || target > 3) {
            throw new BizException("目标状态不合法");
        }
        entity.setStatus(target);
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Contract entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        removeById(id);
    }

    public ContractVO detail(Long id) {
        Contract entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "合同不存在");
        }
        ContractVO vo = toVO(entity);
        fillCustomerNames(List.of(vo));
        return vo;
    }

    public PageResult<ContractVO> pageQuery(ContractQuery query) {
        Page<Contract> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Contract>lambdaQuery()
                        .eq(query.getCustomerId() != null, Contract::getCustomerId, query.getCustomerId())
                        .eq(query.getOpportunityId() != null, Contract::getOpportunityId, query.getOpportunityId())
                        .eq(query.getStatus() != null, Contract::getStatus, query.getStatus())
                        .like(StringUtils.hasText(query.getContractNo()), Contract::getContractNo, query.getContractNo())
                        .orderByDesc(Contract::getId));
        PageResult<ContractVO> result = PageResult.of(page, this::toVO);
        fillCustomerNames(result.records());
        return result;
    }

    private String resolveNo(String contractNo, Long excludeId) {
        if (!StringUtils.hasText(contractNo)) {
            return "CT-" + LocalDateTime.now().format(NO_FMT) + "-"
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
        Contract same = getOne(Wrappers.<Contract>lambdaQuery()
                .eq(Contract::getContractNo, contractNo)
                .ne(excludeId != null, Contract::getId, excludeId)
                .last("limit 1"));
        if (same != null) {
            throw new BizException("合同编号已存在: " + contractNo);
        }
        return contractNo;
    }

    private void applyForm(Contract entity, ContractForm form) {
        entity.setCustomerId(form.getCustomerId());
        entity.setOpportunityId(form.getOpportunityId());
        entity.setAmount(form.getAmount());
        entity.setSignDate(form.getSignDate());
        entity.setRemark(form.getRemark());
    }

    private void fillCustomerNames(List<ContractVO> vos) {
        List<Long> ids = vos.stream().map(ContractVO::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = ids.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        for (ContractVO vo : vos) {
            vo.setCustomerName(nameMap.get(vo.getCustomerId()));
        }
    }

    private ContractVO toVO(Contract entity) {
        ContractVO vo = new ContractVO();
        vo.setId(entity.getId());
        vo.setContractNo(entity.getContractNo());
        vo.setCustomerId(entity.getCustomerId());
        vo.setOpportunityId(entity.getOpportunityId());
        vo.setAmount(entity.getAmount());
        vo.setSignDate(entity.getSignDate());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(contractStatusName(entity.getStatus()));
        vo.setOwnerId(entity.getOwnerId());
        vo.setDeptId(entity.getDeptId());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String contractStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "生效";
            case 2 -> "已归档";
            case 3 -> "已作废";
            default -> String.valueOf(status);
        };
    }
}
