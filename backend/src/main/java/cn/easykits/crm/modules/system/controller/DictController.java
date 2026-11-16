package cn.easykits.crm.modules.system.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.entity.SysDict;
import cn.easykits.crm.entity.SysDictItem;
import cn.easykits.crm.modules.system.dto.DictForm;
import cn.easykits.crm.modules.system.dto.DictItemForm;
import cn.easykits.crm.modules.system.service.DictService;
import cn.easykits.crm.modules.system.vo.DictItemVO;
import cn.easykits.crm.modules.system.vo.DictVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理 - 数据字典（类型 + 字典项维护）。
 */
@Tag(name = "系统-数据字典")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @Operation(summary = "字典类型分页")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<PageResult<DictVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "20") long pageSize,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(dictService.page(pageNum, pageSize, keyword));
    }

    @Operation(summary = "新增字典类型")
    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:create')")
    public Result<SysDict> create(@Valid @RequestBody DictForm form) {
        return Result.ok(dictService.create(form));
    }

    @Operation(summary = "编辑字典类型")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    public Result<SysDict> update(@PathVariable Long id, @Valid @RequestBody DictForm form) {
        return Result.ok(dictService.update(id, form));
    }

    @Operation(summary = "删除字典类型（级联删除字典项，内置字典不可删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "字典项列表（管理视图，含停用）")
    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<List<DictItemVO>> listItems(@PathVariable Long id) {
        return Result.ok(dictService.listItems(id));
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('system:dict:create')")
    public Result<SysDictItem> createItem(@PathVariable Long id, @Valid @RequestBody DictItemForm form) {
        return Result.ok(dictService.createItem(id, form));
    }

    @Operation(summary = "编辑字典项")
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    public Result<Void> updateItem(@PathVariable Long itemId, @Valid @RequestBody DictItemForm form) {
        dictService.updateItem(itemId, form);
        return Result.ok();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        dictService.deleteItem(itemId);
        return Result.ok();
    }
}
