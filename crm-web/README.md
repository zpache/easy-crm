# easy-crm-web

CRM 管理后台前端，基于 **Vben Admin 5.7.0（web-ele 变体）**：Vue 3 · Element Plus · TypeScript · Vite · Pinia · pnpm monorepo。

本目录自 easy-mall 的 `web-admin` 引导而来（同一套 Vben 骨架），仅改了项目名与应用标题（`easy-crm-web` / `Easy CRM Admin`），后续按 CRM 菜单与页面改造。

## 结构

```
crm-web/
├── apps/web-ele/        # 主应用（开发与构建入口）
├── packages/            # @vben 系列共享包（ui-kit、effects、stores、locales 等）
├── internal/            # vite-config、lint-config、tsconfig 等工程配置
└── turbo.json / pnpm-workspace.yaml
```

## 常用命令

```bash
pnpm install
pnpm dev            # 开发（@vben/web-ele）
pnpm build          # 生产构建
pnpm check          # 循环依赖 / 依赖 / 类型 / cspell 检查
```

## 与后端对接

- 前端开发环境通过 `/api` 代理（vite rewrite 剥离前缀），后端已无 context-path；生产 `VITE_GLOB_API_URL=https://crm-api.easykits.cn`；Vben 请求层配置在 `apps/web-ele/src/api`；
- 统一响应 `{ code, message, data }`，成功码 0；
- 登录后动态菜单路由由后端下发（参照 easy-mall 的 `/system/menu/routes` + `/user/codes` 按钮权限模式）。
