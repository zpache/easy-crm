package com.easykits.crm.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easykits.crm.common.api.PageResult;
import com.easykits.crm.common.api.ResultCode;
import com.easykits.crm.common.exception.BizException;
import com.easykits.crm.entity.SysRole;
import com.easykits.crm.entity.SysUserRole;
import com.easykits.crm.mapper.SysRoleMapper;
import com.easykits.crm.mapper.SysUserRoleMapper;
import com.easykits.crm.modules.system.dto.RoleForm;
import com.easykits.crm.modules.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理：分页查询 + 菜单授权。
 */
@Service
@RequiredArgsConstructor
public class RoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    private final SysUserRoleMapper sysUserRoleMapper;
    private final MenuService menuService;

    public PageResult<RoleVO> pageQuery(long pageNum, long pageSize, String name, String code, Integer status) {
        Page<SysRole> page = page(Page.of(pageNum, pageSize), Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(name), SysRole::getName, name)
                .like(StringUtils.hasText(code), SysRole::getCode, code)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
        List<SysRole> records = page.getRecords();
        Map<Long, List<Long>> permissionsMap = records.isEmpty()
                ? Map.of()
                : records.stream().collect(Collectors.toMap(
                        SysRole::getId,
                        role -> menuService.listMenuIdsByRole(role.getId())));
        return PageResult.of(page, role -> toVO(role, permissionsMap.getOrDefault(role.getId(), List.of())));
    }

    /**
     * 启用中的角色（用户表单下拉、分配角色用）。
     */
    public List<RoleVO> listEnabled() {
        return list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId))
                .stream()
                .map(role -> toVO(role, List.of()))
                .toList();
    }

    public SysRole create(RoleForm form) {
        validateCodeUnique(form.code(), null);
        SysRole role = new SysRole();
        applyForm(role, form);
        save(role);
        menuService.replaceRoleMenus(role.getId(), form.permissions());
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysRole update(Long id, RoleForm form) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        validateCodeUnique(form.code(), id);
        applyForm(role, form);
        if (!updateById(role)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        menuService.replaceRoleMenus(id, form.permissions());
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        Long boundUsers = sysUserRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
        if (boundUsers != null && boundUsers > 0) {
            throw new BizException("该角色已分配给用户，无法删除");
        }
        removeById(id);
        menuService.replaceRoleMenus(id, List.of());
    }

    public void changeStatus(Long id, Integer status) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        role.setStatus(status == null ? 1 : status);
        if (!updateById(role)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 按用户查角色码，供登录鉴权使用。
     */
    public List<String> listCodesByUser(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return list(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort))
                .stream().map(SysRole::getCode).toList();
    }

    private void validateCodeUnique(String code, Long excludeId) {
        long exists = count(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, code)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (exists > 0) {
            throw new BizException("角色标识已存在");
        }
    }

    private void applyForm(SysRole role, RoleForm form) {
        role.setName(form.name());
        role.setCode(form.code());
        // 数据范围：1 本人 / 2 本部门 / 3 本部门及下级 / 4 全部；不传默认最严格的本人
        role.setDataScope(form.dataScope() == null ? 1 : form.dataScope());
        role.setSort(form.sort() == null ? 0 : form.sort());
        role.setStatus(form.status() == null ? 1 : form.status());
        role.setRemark(form.remark());
    }

    private RoleVO toVO(SysRole role, List<Long> permissions) {
        return new RoleVO(role.getId(), role.getName(), role.getCode(), role.getDataScope(),
                role.getSort(), role.getStatus(), role.getRemark(), role.getCreateTime(),
                permissions, role.getVersion());
    }
}
