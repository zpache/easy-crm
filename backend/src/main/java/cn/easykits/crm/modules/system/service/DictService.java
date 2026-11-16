package cn.easykits.crm.modules.system.service;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.entity.SysDict;
import cn.easykits.crm.entity.SysDictItem;
import cn.easykits.crm.mapper.SysDictItemMapper;
import cn.easykits.crm.mapper.SysDictMapper;
import cn.easykits.crm.modules.system.constant.DictDefaults;
import cn.easykits.crm.modules.system.dto.DictForm;
import cn.easykits.crm.modules.system.dto.DictItemForm;
import cn.easykits.crm.modules.system.vo.DictItemVO;
import cn.easykits.crm.modules.system.vo.DictVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据字典服务。
 * <p>读路径带进程内缓存（key = dictType）；任何写操作整体失效缓存。
 * 字典表无有效字典项时回退 {@link DictDefaults}（与原常量类取值一致），
 * 保证「删光字典项」不会导致业务取值中断。
 */
@Service
@RequiredArgsConstructor
public class DictService extends ServiceImpl<SysDictMapper, SysDict> {

    private final SysDictItemMapper dictItemMapper;

    /** dictType -> 字典项缓存（写操作整体清空，避免精细失效出错） */
    private final ConcurrentHashMap<String, List<DictItemVO>> cache = new ConcurrentHashMap<>();

    // ============================================================ 读（带缓存 + fallback）

    /**
     * 生效字典项（启用状态，按 sort_no 排序）。表内无数据时回退内置默认值。
     */
    public List<DictItemVO> itemsOfType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return List.of();
        }
        return cache.computeIfAbsent(dictType, this::loadItems);
    }

    private List<DictItemVO> loadItems(String dictType) {
        SysDict dict = getByType(dictType);
        if (dict == null || (dict.getStatus() != null && dict.getStatus() == 0)) {
            // 字典停用或不存在：回退内置默认值
            return DictDefaults.itemsOf(dictType);
        }
        List<SysDictItem> items = dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSortNo)
                .orderByAsc(SysDictItem::getId));
        if (items.isEmpty()) {
            return DictDefaults.itemsOf(dictType);
        }
        return items.stream()
                .map(i -> new DictItemVO(i.getId(), i.getDictType(), i.getLabel(), i.getValue(), i.getSortNo(), i.getStatus()))
                .toList();
    }

    /**
     * value -> label（列表页展示名称用；字典与内置默认值都查不到返回 null）。
     */
    public String labelOf(String dictType, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return itemsOfType(dictType).stream()
                .filter(i -> value.equals(i.value()))
                .map(DictItemVO::label)
                .findFirst()
                .orElse(null);
    }

    /** 字典（含内置 fallback）是否包含指定 value */
    public boolean hasValue(String dictType, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return itemsOfType(dictType).stream().anyMatch(i -> value.equals(i.value()));
    }

    /** 字典类型是否存在且启用 */
    public boolean isTypeEnabled(String dictType) {
        SysDict dict = getByType(dictType);
        return dict != null && (dict.getStatus() == null || dict.getStatus() == 1);
    }

    // ============================================================ 管理端：字典类型

    /**
     * 字典类型分页（keyword 匹配编码 / 名称），附带字典项数量。
     */
    public PageResult<DictVO> page(long pageNum, long pageSize, String keyword) {
        Page<SysDict> page = page(Page.of(pageNum, pageSize), Wrappers.<SysDict>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysDict::getDictType, keyword)
                        .or()
                        .like(SysDict::getName, keyword))
                .orderByAsc(SysDict::getDictType));
        Map<String, Long> counts = dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                        .in(SysDictItem::getDictType, page.getRecords().stream().map(SysDict::getDictType).toList()))
                .stream()
                .collect(Collectors.groupingBy(SysDictItem::getDictType, Collectors.counting()));
        return PageResult.of(page, d -> new DictVO(d.getId(), d.getDictType(), d.getName(), d.getRemark(),
                d.getBuiltIn(), d.getStatus(), counts.getOrDefault(d.getDictType(), 0L), d.getUpdateTime()));
    }

    /**
     * 新增字典类型。dict_type 全局唯一（逻辑删除占位由 Service 校验规避）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysDict create(DictForm form) {
        if (getByType(form.dictType()) != null) {
            throw new BizException(ResultCode.ACCOUNT_EXISTS, "字典类型编码已存在: " + form.dictType());
        }
        SysDict dict = new SysDict();
        dict.setDictType(form.dictType());
        dict.setName(form.name());
        dict.setRemark(form.remark());
        dict.setBuiltIn(0);
        dict.setStatus(form.status() == null ? 1 : form.status());
        if (!save(dict)) {
            throw new BizException(ResultCode.DATA_CONFLICT);
        }
        cache.clear();
        return dict;
    }

    /**
     * 编辑字典类型（编码不可改）。内置字典允许改名称 / 备注 / 状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysDict update(Long id, DictForm form) {
        SysDict dict = getById(id);
        if (dict == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (form.dictType() != null && !form.dictType().equals(dict.getDictType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典类型编码不允许修改");
        }
        dict.setName(form.name());
        dict.setRemark(form.remark());
        dict.setStatus(form.status() == null ? dict.getStatus() : form.status());
        if (!updateById(dict)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        cache.clear();
        return dict;
    }

    /**
     * 删除字典类型（级联逻辑删除字典项）。内置字典不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDict dict = getById(id);
        if (dict == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (dict.getBuiltIn() != null && dict.getBuiltIn() == 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "内置字典不允许删除，可停用");
        }
        List<SysDictItem> items = dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dict.getDictType()));
        items.forEach(item -> {
            if (dictItemMapper.deleteById(item.getId()) <= 0) {
                throw new BizException(ResultCode.DATA_CONFLICT, "字典项删除冲突，请刷新后重试");
            }
        });
        if (!removeById(id)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        cache.clear();
    }

    // ============================================================ 管理端：字典项

    /** 字典全部字典项（管理视图：含停用，按 sort_no 排序；按 dict_type 查询，种子行 dict_id 冗余列允许为空） */
    public List<DictItemVO> listItems(Long dictId) {
        SysDict dict = getById(dictId);
        if (dict == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        List<SysDictItem> items = dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dict.getDictType())
                .orderByAsc(SysDictItem::getSortNo)
                .orderByAsc(SysDictItem::getId));
        return items.stream()
                .map(i -> new DictItemVO(i.getId(), i.getDictType(), i.getLabel(), i.getValue(), i.getSortNo(), i.getStatus()))
                .toList();
    }

    /**
     * 新增字典项。dict_type + value 唯一由 Service 校验；内置字典允许追加自定义项。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysDictItem createItem(Long dictId, DictItemForm form) {
        SysDict dict = getById(dictId);
        if (dict == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        long exists = dictItemMapper.selectCount(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dict.getDictType())
                .eq(SysDictItem::getValue, form.value()));
        if (exists > 0) {
            throw new BizException(ResultCode.ACCOUNT_EXISTS, "字典项值已存在: " + form.value());
        }
        SysDictItem item = new SysDictItem();
        item.setDictId(dictId);
        item.setDictType(dict.getDictType());
        item.setLabel(form.label());
        item.setValue(form.value());
        item.setSortNo(form.sortNo() == null ? 0 : form.sortNo());
        item.setStatus(form.status() == null ? 1 : form.status());
        item.setRemark(form.remark());
        if (dictItemMapper.insert(item) <= 0) {
            throw new BizException(ResultCode.DATA_CONFLICT, "字典项创建失败");
        }
        cache.clear();
        return item;
    }

    /**
     * 编辑字典项（value 不可改，避免历史业务数据失配）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long itemId, DictItemForm form) {
        SysDictItem item = dictItemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (form.value() != null && !form.value().equals(item.getValue())) {
            throw new BizException(ResultCode.PARAM_ERROR, "字典项值不允许修改（历史数据兼容），如需调整请新增后停用旧项");
        }
        item.setLabel(form.label());
        item.setSortNo(form.sortNo() == null ? item.getSortNo() : form.sortNo());
        item.setStatus(form.status() == null ? item.getStatus() : form.status());
        item.setRemark(form.remark());
        if (dictItemMapper.updateById(item) <= 0) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        cache.clear();
    }

    /**
     * 删除字典项。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        SysDictItem item = dictItemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        if (dictItemMapper.deleteById(itemId) <= 0) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        cache.clear();
    }

    // ============================================================ 私有工具

    private SysDict getByType(String dictType) {
        return getOne(Wrappers.<SysDict>lambdaQuery().eq(SysDict::getDictType, dictType), false);
    }
}
