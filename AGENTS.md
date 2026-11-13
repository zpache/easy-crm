# easy-crm 仓库工作约定

## Git 提交规范

- 每完成一个功能/修复，**必须自动提交并 push 到远程**，不要等用户提醒：
  - `git add -A && git commit -m "<feat|fix|chore|docs>(<scope>): <中文描述>" && git push`
  - 提交信息用中文描述，遵循 conventional commits 前缀（feat / fix / chore / docs / refactor / test）
- 默认分支 `main`，远程 `origin = git@github.com:zpache/easy-crm.git`
- 仓库作者（local 配置，勿改全局）：`user.name=zpache`，`user.email=ryanwang_best@126.com`
- 提交前确认 `.gitignore` 生效：`target/`、`node_modules/`、`dist/`、`logs/`、`application-local.yml`、`.env.local` 等不得入库
- 若 push 因 SSH 失败，如实报告错误，不要伪造成功

## 功能菜单规范（必须遵守）

- **每新增一个前端页面/功能模块，必须同步完成菜单配置**：
  1. 在 `backend/db/init.sql` 的 `sys_permission` 种子段新增目录/菜单行（name 唯一、component 指向 `src/views` 下相对路径）；
  2. 同步执行到当前数据库（菜单是每次请求实时读取的，无需重启后端，前端刷新页面即可见）；
  3. 若有按钮级权限，把权限码同时登记到后端 `PermissionCodes.ALL` 与前端 `src/constants/permissions.ts`。
- **授权给超级管理员**：admin 角色（code=admin）走代码兜底自动获得全部启用菜单与权限码（`MenuService.resolveVisibleMenuIds`），因此只需保证菜单行 `status=1`，不需要插入 sys_role_permission。
- 注意：逻辑删除过的菜单行会占用自增 id，`ON DUPLICATE KEY UPDATE` 种子脚本会污染已删行；若 id 撞上已删行，先物理 DELETE 再执行种子脚本。

## 项目结构

- `backend/`：Java 25 + Spring Boot 4.1.2 + MyBatis-Plus 3.5.18，单模块 `com.easykits.crm`
- `crm-web/`：Vben Admin 5.7.0（web-ele），pnpm monorepo
- `backend/db/init.sql`：建表 + 初始数据（admin/admin123，仅开发环境）
