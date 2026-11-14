package cn.easykits.crm.modules.product.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.PriceList;
import cn.easykits.crm.entity.Product;
import cn.easykits.crm.mapper.PriceListMapper;
import cn.easykits.crm.mapper.ProductMapper;
import cn.easykits.crm.modules.product.dto.PriceListForm;
import cn.easykits.crm.modules.product.vo.PriceListVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 价目表服务：CRUD / 分页 / 生效价查询（全部走 LambdaQueryWrapper，无 SQL 字符串拼接）。
 */
@Service
@RequiredArgsConstructor
public class PriceListServiceImpl extends ServiceImpl<PriceListMapper, PriceList> {

    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(PriceListForm form) {
        assertProduct(form.getProductId());
        assertDateRange(form.getEffectiveDate(), form.getExpireDate());
        PriceList entity = new PriceList();
        applyForm(entity, form);
        entity.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        save(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, PriceListForm form) {
        PriceList entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "价格记录不存在");
        }
        assertProduct(form.getProductId());
        assertDateRange(form.getEffectiveDate(), form.getExpireDate());
        applyForm(entity, form);
        entity.setStatus(form.getStatus() == null ? entity.getStatus() : form.getStatus());
        if (!updateById(entity)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PriceList entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "价格记录不存在");
        }
        removeById(id);
    }

    public PriceListVO detail(Long id) {
        PriceList entity = getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "价格记录不存在");
        }
        PriceListVO vo = toVO(entity);
        fillProductNames(List.of(vo));
        return vo;
    }

    public PageResult<PriceListVO> pageQuery(long pageNum, long pageSize, Long productId, Integer status) {
        Page<PriceList> page = page(Page.of(pageNum, pageSize),
                Wrappers.<PriceList>lambdaQuery()
                        .eq(productId != null, PriceList::getProductId, productId)
                        .eq(status != null, PriceList::getStatus, status)
                        .orderByDesc(PriceList::getEffectiveDate)
                        .orderByDesc(PriceList::getId));
        PageResult<PriceListVO> result = PageResult.of(page, this::toVO);
        fillProductNames(result.records());
        return result;
    }

    /**
     * 指定日期生效的价格：status=1 且（生效日为空或 <= 指定日）且（失效日为空或 >= 指定日）。
     * date 为空取当天。
     */
    public List<PriceListVO> effectivePrices(Long productId, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<PriceList> list = list(Wrappers.<PriceList>lambdaQuery()
                .eq(PriceList::getProductId, productId)
                .eq(PriceList::getStatus, 1)
                .and(w -> w.isNull(PriceList::getEffectiveDate)
                        .or().le(PriceList::getEffectiveDate, day))
                .and(w -> w.isNull(PriceList::getExpireDate)
                        .or().ge(PriceList::getExpireDate, day))
                .orderByDesc(PriceList::getEffectiveDate));
        List<PriceListVO> vos = list.stream().map(this::toVO).toList();
        fillProductNames(vos);
        return vos;
    }

    private void assertProduct(Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
    }

    private void assertDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException("生效日期不能晚于失效日期");
        }
    }

    private void applyForm(PriceList entity, PriceListForm form) {
        entity.setProductId(form.getProductId());
        entity.setPrice(form.getPrice());
        entity.setEffectiveDate(form.getEffectiveDate());
        entity.setExpireDate(form.getExpireDate());
        entity.setRemark(form.getRemark());
    }

    private void fillProductNames(List<PriceListVO> vos) {
        List<Long> ids = vos.stream().map(PriceListVO::getProductId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = ids.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        for (PriceListVO vo : vos) {
            vo.setProductName(nameMap.get(vo.getProductId()));
        }
    }

    private PriceListVO toVO(PriceList entity) {
        PriceListVO vo = new PriceListVO();
        vo.setId(entity.getId());
        vo.setProductId(entity.getProductId());
        vo.setPrice(entity.getPrice());
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
