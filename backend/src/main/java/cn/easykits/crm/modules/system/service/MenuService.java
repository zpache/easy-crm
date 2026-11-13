package cn.easykits.crm.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.crm.common.api.ResultCode;
import cn.easykits.crm.common.exception.BizException;
import cn.easykits.crm.common.permission.PermissionCodes;
import cn.easykits.crm.entity.SysPermission;
import cn.easykits.crm.entity.SysRole;
import cn.easykits.crm.entity.SysRolePermission;
import cn.easykits.crm.entity.SysUser;
import cn.easykits.crm.entity.SysUserRole;
import cn.easykits.crm.mapper.SysPermissionMapper;
import cn.easykits.crm.mapper.SysRoleMapper;
import cn.easykits.crm.mapper.SysRolePermissionMapper;
import cn.easykits.crm.mapper.SysUserMapper;
import cn.easykits.crm.mapper.SysUserRoleMapper;
import cn.easykits.crm.modules.system.dto.MenuForm;
import cn.easykits.crm.modules.system.vo.MenuMeta;
import cn.easykits.crm.modules.system.vo.MenuRouteVO;
import cn.easykits.crm.modules.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单 / 权限管理（移植自 easy-mall admin，存储表为 sys_permission）。
 * 树的每个节点对应 vben 的一条路由 + meta。
 */
@Service
@RequiredArgsConstructor
public class MenuService extends ServiceImpl<SysPermissionMapper, SysPermission> {

    /** 外链 / 内嵌菜单统一使用 vben 的 IFrameView 组件渲染 */
    private static final String IFRAME_VIEW = "IFrameView";

    private static final String TYPE_BUTTON = "button";
    private static final String TYPE_CATALOG = "catalog";
    private static final String TYPE_LINK = "link";
    private static final String TYPE_EMBEDDED = "embedded";
    private static final String ROLE_ADMIN = "ADMIN";

    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 全量菜单树（菜单管理页使用）。
     */
    public List<MenuVO> listTree() {
        List<SysPermission> all = list(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getPid)
                .orderByAsc(SysPermission::getOrderNo)
                .orderByAsc(SysPermission::getId));
        return buildTree(all, 0L);
    }

    /**
     * 当前用户拥有的菜单树（按角色 → 菜单关联过滤，只保留启用的节点）。
     */
    public List<MenuVO> listTreeByUser(Long userId) {
        Set<Long> visibleIds = resolveVisibleMenuIds(userId);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        List<SysPermission> all = listEnabled().stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .toList();
        return buildTree(all, 0L);
    }

    /**
     * 当前用户的权限码集合（菜单、按钮上的 auth_code）。
     */
    public List<String> listAuthCodesByUser(Long userId) {
        Set<Long> visibleIds = resolveVisibleMenuIds(userId);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        Set<String> codes = listEnabled().stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .map(SysPermission::getAuthCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // ADMIN 默认拥有代码中登记的全部权限，避免新增按钮权限后管理员被锁死
        if (hasAdminRole(resolveRoleIds(userId))) {
            codes.addAll(PermissionCodes.ALL);
        }
        return List.copyOf(codes);
    }

    /**
     * 当前用户的路由树，字段与 vue-router 的路由记录一一对应，前端直接交给
     * vben 的 generateAccessible 生成路由和菜单（accessMode = backend）。
     * <p>
     * 与 {@link #listTreeByUser(Long)} 的区别：这里过滤掉按钮，并把外链 / 内嵌
     * 菜单的 component 统一映射为 IFrameView。
     */
    public List<MenuRouteVO> listRoutesByUser(Long userId) {
        Set<Long> visibleIds = resolveVisibleMenuIds(userId);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysPermission> byId = listEnabled().stream()
                .collect(Collectors.toMap(SysPermission::getId, menu -> menu, (a, b) -> a, LinkedHashMap::new));
        return buildRouteChildren(0L, byId, visibleIds);
    }

    /**
     * 用户的默认首页：路由树中第一个可访问的页面路径，找不到时返回 null 由前端兜底。
     */
    public String resolveHomePath(Long userId) {
        return firstPagePath(listRoutesByUser(userId));
    }

    /**
     * 当前用户可见的菜单 ID 集合：角色已授权的菜单 + 其祖先链（保证子菜单不会因为
     * 父级未授权而在建树时丢失）。超级管理员（ADMIN）直接获得全部启用菜单。
     */
    public Set<Long> resolveVisibleMenuIds(Long userId) {
        List<SysPermission> all = listEnabled();
        if (all.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = resolveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        if (hasAdminRole(roleIds)) {
            return all.stream().map(SysPermission::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        Set<Long> granted = sysRolePermissionMapper.selectList(
                        Wrappers.<SysRolePermission>lambdaQuery().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toSet());
        if (granted.isEmpty()) {
            return Set.of();
        }
        Map<Long, SysPermission> byId = all.stream()
                .collect(Collectors.toMap(SysPermission::getId, menu -> menu, (a, b) -> a));
        Set<Long> visible = new LinkedHashSet<>();
        for (Long menuId : granted) {
            SysPermission current = byId.get(menuId);
            // 沿 pid 向上补齐祖先链，父级停用时中断（父级不可见则子级也不可见）
            while (current != null && visible.add(current.getId())) {
                current = byId.get(current.getPid());
            }
        }
        return visible;
    }

    public SysPermission create(MenuForm form) {
        validateUnique(form.name(), form.path(), null);
        SysPermission menu = new SysPermission();
        applyForm(menu, form);
        save(menu);
        return menu;
    }

    public SysPermission update(Long id, MenuForm form) {
        SysPermission menu = getById(id);
        if (menu == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        if (Objects.equals(id, form.pid())) {
            throw new BizException("上级菜单不能是自己");
        }
        validateUnique(form.name(), form.path(), id);
        applyForm(menu, form);
        if (!updateById(menu)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysPermission menu = getById(id);
        if (menu == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        long children = count(Wrappers.<SysPermission>lambdaQuery().eq(SysPermission::getPid, id));
        if (children > 0) {
            throw new BizException("请先删除下级菜单");
        }
        removeById(id);
        // 同步清理角色授权关系
        sysRolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getPermissionId, id));
    }

    public boolean nameExists(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return count(Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getName, name)
                .ne(excludeId != null, SysPermission::getId, excludeId)) > 0;
    }

    public boolean pathExists(String path, Long excludeId) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return count(Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getPath, path)
                .ne(excludeId != null, SysPermission::getId, excludeId)) > 0;
    }

    /**
     * 按角色查询已授权菜单 ID（授权弹窗回显用）。
     */
    public List<Long> listMenuIdsByRole(Long roleId) {
        return sysRolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).toList();
    }

    /**
     * 覆盖式同步角色授权：先清空再写入。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        sysRolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        Set<Long> distinct = menuIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long menuId : distinct) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(menuId);
            sysRolePermissionMapper.insert(relation);
        }
    }

    private void applyForm(SysPermission menu, MenuForm form) {
        menu.setPid(form.pid() == null ? 0L : form.pid());
        menu.setName(form.name());
        menu.setType(form.type());
        menu.setAuthCode(form.authCode());
        menu.setPath(form.path());
        menu.setComponent(form.component());
        menu.setStatus(form.status() == null ? 1 : form.status());

        MenuForm.MenuMetaForm meta = form.meta();
        if (meta == null) {
            menu.setTitle(form.name());
            menu.setOrderNo(0);
            return;
        }
        menu.setTitle(StringUtils.hasText(meta.title()) ? meta.title() : form.name());
        menu.setIcon(meta.icon());
        menu.setOrderNo(meta.order() == null ? 0 : meta.order());
        menu.setBadge(meta.badge());
        menu.setBadgeType(meta.badgeType());
        menu.setBadgeVariants(meta.badgeVariants());
        menu.setKeepAlive(toFlag(meta.keepAlive()));
        menu.setAffixTab(toFlag(meta.affixTab()));
        menu.setHideInMenu(toFlag(meta.hideInMenu()));
        menu.setHideChildrenInMenu(toFlag(meta.hideChildrenInMenu()));
        menu.setHideInBreadcrumb(toFlag(meta.hideInBreadcrumb()));
        menu.setHideInTab(toFlag(meta.hideInTab()));
        menu.setLink(meta.link());
        menu.setIframeSrc(meta.iframeSrc());
    }

    private void validateUnique(String name, String path, Long excludeId) {
        if (nameExists(name, excludeId)) {
            throw new BizException("菜单名称已存在");
        }
        if (pathExists(path, excludeId)) {
            throw new BizException("路由地址已存在");
        }
    }

    private int toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private boolean toBoolean(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 全部启用菜单，按排序值、ID 升序，保证建树顺序稳定。
     */
    private List<SysPermission> listEnabled() {
        return list(Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getOrderNo)
                .orderByAsc(SysPermission::getId));
    }

    /**
     * 用户的角色 ID：优先取 sys_user_role，账号未配置关联时回退到 sys_user.role 角色码，
     * 兼容早期只有单角色字段的数据。
     */
    private List<Long> resolveRoleIds(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (!roleIds.isEmpty()) {
            return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                            .in(SysRole::getId, roleIds)
                            .eq(SysRole::getStatus, 1))
                    .stream().map(SysRole::getId).toList();
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return List.of();
        }
        SysRole role = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getCode, user.getRole())
                        .eq(SysRole::getStatus, 1)
                        .last("limit 1"));
        return role == null ? List.of() : List.of(role.getId());
    }

    private boolean hasAdminRole(List<Long> roleIds) {
        return sysRoleMapper.selectList(
                        Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds))
                .stream()
                .anyMatch(role -> ROLE_ADMIN.equalsIgnoreCase(role.getCode()));
    }

    private List<MenuRouteVO> buildRouteChildren(Long pid, Map<Long, SysPermission> byId, Set<Long> visibleIds) {
        List<MenuRouteVO> result = new ArrayList<>();
        for (SysPermission menu : byId.values()) {
            if (!Objects.equals(menu.getPid() == null ? 0L : menu.getPid(), pid)) {
                continue;
            }
            if (!visibleIds.contains(menu.getId()) || TYPE_BUTTON.equals(menu.getType())) {
                continue;
            }
            List<MenuRouteVO> children = buildRouteChildren(menu.getId(), byId, visibleIds);
            // 目录下没有任何可访问页面时不渲染，避免菜单树出现空节点
            if (TYPE_CATALOG.equals(menu.getType()) && children.isEmpty()) {
                continue;
            }
            result.add(new MenuRouteVO(
                    menu.getName(),
                    menu.getPath(),
                    resolveComponent(menu),
                    null,
                    toMeta(menu),
                    children));
        }
        return result;
    }

    /**
     * 外链 / 内嵌菜单统一交给 IFrameView 渲染，其余按库中配置的组件路径解析页面。
     */
    private String resolveComponent(SysPermission menu) {
        if (TYPE_LINK.equals(menu.getType()) || TYPE_EMBEDDED.equals(menu.getType())) {
            return IFRAME_VIEW;
        }
        return menu.getComponent();
    }

    private String firstPagePath(List<MenuRouteVO> routes) {
        for (MenuRouteVO route : routes) {
            if (StringUtils.hasText(route.component())
                    && StringUtils.hasText(route.path())
                    && !IFRAME_VIEW.equals(route.component())) {
                return route.path();
            }
            String child = firstPagePath(route.children());
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private List<MenuVO> buildTree(List<SysPermission> all, Long pid) {
        Map<Long, List<SysPermission>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(menu -> menu.getPid() == null ? 0L : menu.getPid()));
        return buildChildren(childrenMap, pid);
    }

    private List<MenuVO> buildChildren(Map<Long, List<SysPermission>> childrenMap, Long pid) {
        List<SysPermission> children = childrenMap.get(pid);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<MenuVO> result = new ArrayList<>(children.size());
        for (SysPermission menu : children) {
            result.add(new MenuVO(
                    menu.getId(),
                    menu.getPid(),
                    menu.getName(),
                    menu.getType(),
                    menu.getAuthCode(),
                    menu.getPath(),
                    menu.getComponent(),
                    menu.getStatus(),
                    toMeta(menu),
                    buildChildren(childrenMap, menu.getId())));
        }
        return result;
    }

    private MenuMeta toMeta(SysPermission menu) {
        return new MenuMeta(
                menu.getTitle(),
                menu.getIcon(),
                menu.getOrderNo(),
                menu.getBadge(),
                menu.getBadgeType(),
                menu.getBadgeVariants(),
                toBoolean(menu.getKeepAlive()),
                toBoolean(menu.getAffixTab()),
                toBoolean(menu.getHideInMenu()),
                toBoolean(menu.getHideChildrenInMenu()),
                toBoolean(menu.getHideInBreadcrumb()),
                toBoolean(menu.getHideInTab()),
                menu.getLink(),
                menu.getIframeSrc());
    }
}
