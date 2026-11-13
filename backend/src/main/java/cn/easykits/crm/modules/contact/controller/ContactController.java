package cn.easykits.crm.modules.contact.controller;

import cn.easykits.crm.common.api.PageResult;
import cn.easykits.crm.common.api.Result;
import cn.easykits.crm.modules.contact.dto.ContactCreateForm;
import cn.easykits.crm.modules.contact.dto.ContactQuery;
import cn.easykits.crm.modules.contact.service.ContactServiceImpl;
import cn.easykits.crm.modules.contact.vo.ContactVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 联系人管理（可见性随客户，不注册行级数据权限）。
 */
@Tag(name = "联系人管理")
@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactServiceImpl contactService;

    @Operation(summary = "创建联系人（可标记主联系人）")
    @PostMapping
    @PreAuthorize("hasAuthority('crm:contact:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ContactCreateForm form) {
        Long id = contactService.createFromForm(form);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "更新联系人")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:contact:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContactCreateForm form) {
        contactService.updateFromForm(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除联系人")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:contact:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "联系人分页（customerId 按客户查询）")
    @GetMapping
    @PreAuthorize("hasAuthority('crm:contact:list')")
    public Result<PageResult<ContactVO>> page(ContactQuery query) {
        return Result.ok(contactService.pageQuery(query));
    }

    @Operation(summary = "设为主联系人（同客户其余主联系人自动取消）")
    @PutMapping("/{id}/primary")
    @PreAuthorize("hasAuthority('crm:contact:primary')")
    public Result<Void> setPrimary(@PathVariable Long id) {
        contactService.setPrimary(id);
        return Result.ok();
    }
}
