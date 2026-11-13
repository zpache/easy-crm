package com.easykits.crm.modules.system.vo;

import java.util.List;

/**
 * 供前端 vben 动态路由使用的菜单节点，字段与 vue-router 的路由记录对齐。
 * 前端拿到后交给 generateAccessible 生成路由与菜单（accessMode = backend）。
 */
public record MenuRouteVO(
        String name,
        String path,
        String component,
        String redirect,
        MenuMeta meta,
        List<MenuRouteVO> children
) {
}
