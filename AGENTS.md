# easy-crm 仓库工作约定

## Git 提交规范

- 每完成一个功能/修复，**必须自动提交并 push 到远程**，不要等用户提醒：
  - `git add -A && git commit -m "<feat|fix|chore|docs>(<scope>): <中文描述>" && git push`
  - 提交信息用中文描述，遵循 conventional commits 前缀（feat / fix / chore / docs / refactor / test）
- 默认分支 `main`，远程 `origin = git@github.com:zpache/easy-crm.git`
- 仓库作者（local 配置，勿改全局）：`user.name=zpache`，`user.email=ryanwang_best@126.com`
- 提交前确认 `.gitignore` 生效：`target/`、`node_modules/`、`dist/`、`logs/`、`application-local.yml`、`.env.local` 等不得入库
- 若 push 因 SSH 失败，如实报告错误，不要伪造成功

## 项目结构

- `backend/`：Java 25 + Spring Boot 4.1.2 + MyBatis-Plus 3.5.18，单模块 `com.easykits.crm`
- `crm-web/`：Vben Admin 5.7.0（web-ele），pnpm monorepo
- `backend/db/init.sql`：建表 + 初始数据（admin/admin123，仅开发环境）
