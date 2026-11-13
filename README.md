# easy-crm 通用 CRM 系统

前后端分离的通用 CRM：管理后台 RESTful API + Vue 3 前端，后续规划小程序端。

| 子项目 | 说明 | 技术栈 |
| --- | --- | --- |
| [backend/](backend/README.md) | 后端（单模块 + 按业务域分包），端口 8080，context-path=/api | Java 25 · Spring Boot 4.1.2 · MyBatis-Plus 3.5.18 · MySQL 8.4 |
| [crm-web/](crm-web/README.md) | 管理后台前端 | Vben Admin 5.7.0（web-ele）· Vue 3 · Element Plus · pnpm monorepo |
| 小程序 | 待规划 | uni-app（参照 easy-mall miniprogram） |

## 目录结构

```
easy-crm/
├── backend/                    # 后端 Maven 工程（单模块）
│   └── src/main/java/com/easykits/crm/
│       ├── CrmApplication.java
│       ├── common/             # 统一响应 / 异常 / 登录上下文 / 行级数据权限
│       ├── config/             # MyBatis-Plus 插件、审计字段填充、Jackson 格式
│       └── (业务模块按 com.easykits.crm.{module}.{controller|service|mapper|entity|dto|vo} 落地)
├── crm-web/                    # 前端（Vben Admin monorepo，应用在 apps/web-ele）
└── db/                         # 数据库脚本（待补充）
```

## 当前进度

- [x] 后端项目基座：pom.xml / application.yml / Result 统一响应 / BizException / 全局异常处理
- [x] MyBatis-Plus 基础配置：分页（单页上限 200）、乐观锁、防全表更新、create_time/update_time/create_by/update_by 自动填充
- [x] BaseEntity：自增主键 + 审计字段 + version + 逻辑删除
- [x] 行级数据权限拦截器骨架（本人 / 本部门 / 本部门及下级 / 全部），接入说明见 [backend/README.md](backend/README.md)
- [x] 前端项目引导（自 easy-mall web-admin 拷贝的 Vben Admin 5 骨架，待按 CRM 菜单改造）
- [ ] 用户-角色-组织-菜单模块（参照 easy-mall backend 的 system 模块移植）
- [ ] 认证（JWT 过滤器 → LoginUserContext）、客户/联系人/商机等 CRM 业务模块

## 已具备（无需开发）

RBAC 表：sys_user、sys_role、sys_user_role、sys_permission、sys_role_permission；组织表：sys_dept。
后端只需提供 RESTful API，前端已有参考实现。

## 快速启动

```bash
# 后端（需 JDK 25）
cd backend
MYSQL_PASSWORD=xxx mvn spring-boot:run

# 前端（需 pnpm）
cd crm-web
pnpm install
pnpm dev
```
