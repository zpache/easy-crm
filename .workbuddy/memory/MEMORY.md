# easy-crm 项目长期备忘

## 端口与路径约定（2026-11-20 起）
- 后端端口 8085，无 context-path，接口直挂根路径（/auth/login 等）；前端 baseURL 生产为 https://crm-api.easykits.cn，开发走 vite 代理 /api → 127.0.0.1:8085（rewrite 剥离前缀）。

## 生产环境（101.35.43.129）
- 前端：/opt/easy-crm/admin-web，域名 https://crm.easykits.cn（证书已配）。
- 后端：/opt/easy-crm/easy-crm-backend.jar，start.sh 注入环境变量（MYSQL_*、JWT_SECRET、CORS_ALLOWED_ORIGINS、ADMIN_INITIAL_PASSWORD），nohup + profiles=prod，日志 /opt/easy-crm/logs/backend.log；无 systemd。
- 数据库：腾讯云 CDB sh-cdb-rl71esqs.sql.tencentcdb.com:29325 / easy_crm 库 / easy_mall 账号（与 easy-mall 同库实例，MySQL 5.7）；建库脚本 backend/db/init.sql + db/migration/ 幂等迁移。
- nginx：/etc/nginx/conf.d/crm.conf（crm.easykits.cn 已生效；crm-api.easykits.cn server 块注释待证书）。
- 首次部署 admin 初始密码经 ADMIN_INITIAL_PASSWORD 注入（2026-11-20，ehlmM7dNth7nP98KFnQJ），已提醒首登改密。

## 其他
- 服务器 CentOS 7 + yum；easy-bpm 8083、easy-oa 8084、easy-mall admin 8081/api 8082 均在同一台机器，注意端口冲突。
- 待办：crm-api.easykits.cn SSL 证书上传后启用 nginx server 块。
