package cn.easykits.crm.modules.product.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.Product;
import cn.easykits.crm.mapper.ProductMapper;
import cn.easykits.crm.modules.product.constant.ProductDicts;
import cn.easykits.crm.modules.product.dto.ProductForm;
import cn.easykits.crm.modules.product.dto.ProductQuery;
import cn.easykits.crm.modules.product.vo.ProductVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 产品服务：编码唯一校验、金额非负校验、CRUD / 分页。
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> {

    /** 创建产品：编码唯一、金额非负。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromForm(ProductForm form) {
        assertAmounts(form.getCostPrice(), form.getStandardPrice());
        long exists = count(Wrappers.<Product>lambdaQuery()
                .eq(Product::getCode, form.getCode()));
        if (exists > 0) {
            throw new BizException("产品编码已存在: " + form.getCode());
        }
        Product product = new Product();
        applyForm(product, form);
        product.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        save(product);
        return product.getId();
    }

    /** 更新产品：编码唯一（排除自身）、金额非负。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateFromForm(Long id, ProductForm form) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
        assertAmounts(form.getCostPrice(), form.getStandardPrice());
        Product sameCode = getOne(Wrappers.<Product>lambdaQuery()
                .eq(Product::getCode, form.getCode())
                .ne(Product::getId, id)
                .last("limit 1"));
        if (sameCode != null) {
            throw new BizException("产品编码已存在: " + form.getCode());
        }
        applyForm(product, form);
        product.setStatus(form.getStatus() == null ? product.getStatus() : form.getStatus());
        if (!updateById(product)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /** 逻辑删除产品。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
        removeById(id);
    }

    public ProductVO detail(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "产品不存在");
        }
        return toVO(product);
    }

    public PageResult<ProductVO> pageQuery(ProductQuery query) {
        Page<Product> page = page(Page.of(query.getPageNum(), query.getPageSize()),
                Wrappers.<Product>lambdaQuery()
                        .and(StringUtils.hasText(query.getKeyword()), w -> w
                                .like(Product::getName, query.getKeyword())
                                .or().like(Product::getCode, query.getKeyword()))
                        .eq(StringUtils.hasText(query.getCategory()), Product::getCategory, query.getCategory())
                        .eq(query.getStatus() != null, Product::getStatus, query.getStatus())
                        .orderByDesc(Product::getId));
        return PageResult.of(page, this::toVO);
    }

    private void assertAmounts(BigDecimal costPrice, BigDecimal standardPrice) {
        if (costPrice != null && costPrice.signum() < 0) {
            throw new BizException("成本价不能为负数");
        }
        if (standardPrice != null && standardPrice.signum() < 0) {
            throw new BizException("标准价不能为负数");
        }
    }

    private void applyForm(Product product, ProductForm form) {
        product.setCode(form.getCode());
        product.setName(form.getName());
        product.setCategory(form.getCategory());
        product.setSpecification(form.getSpecification());
        product.setUnit(form.getUnit());
        product.setCostPrice(form.getCostPrice());
        product.setStandardPrice(form.getStandardPrice());
        product.setRemark(form.getRemark());
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setCode(product.getCode());
        vo.setName(product.getName());
        vo.setCategory(product.getCategory());
        vo.setSpecification(product.getSpecification());
        vo.setUnit(product.getUnit());
        vo.setStatus(product.getStatus());
        vo.setStatusName(ProductDicts.productStatusName(product.getStatus()));
        vo.setCostPrice(product.getCostPrice());
        vo.setStandardPrice(product.getStandardPrice());
        vo.setRemark(product.getRemark());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }
}
