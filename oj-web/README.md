# Amadeus OJ 管理前端

Vue 3 + TypeScript + Vite + Vue Router + Pinia + Axios + Element Plus。

## 本地启动

在 `D:/code/ideaPrograms/java_steins_gate/java_code/Spring/amadeus-oj/oj-web` 执行，需要 Node.js 22.12+（推荐 Node 24 LTS）。

```powershell
pnpm install
pnpm dev
```

打开 http://127.0.0.1:5173 。使用数据库中已有的管理员账号登录，没有默认账号或公开注册入口。

默认开发代理：`/api/sysUser/*` → `http://127.0.0.1:9201/sysUser/*`。请先启动系统服务及其 MySQL、Redis、Nacos 依赖；以实际启动日志确认端口。

需要修改后端地址时，复制 `.env.example` 为 `.env.local` 并修改 `VITE_API_TARGET`，重启 Vite。

接入网关前，确认 Nacos 中 `/system/**` 路由、去前缀规则和 `/system/sysUser/login` 登录白名单，然后配置：

```dotenv
VITE_API_TARGET=http://127.0.0.1:19090/system
```

## 当前范围

| 页面       | 路由         | 行为                                                            |
| ---------- | ------------ | --------------------------------------------------------------- |
| 管理员登录 | `/login`     | `POST /sysUser/login`，成功响应 `data` 为 JWT                   |
| 工作台     | `/dashboard` | 欢迎信息、当前账号、功能入口；不展示模拟业务统计                |
| 管理员管理 | `/admins`    | `POST /sysUser/add`，提交 `userAccount`、`nickName`、`password` |
| 题目管理   | `/questions` | 待开放说明，无虚构接口调用                                      |
| 竞赛管理   | `/contests`  | 待开放说明，无虚构接口调用                                      |
| 404        | 其他路径     | 返回登录或工作台                                                |

当前后端没有可用的管理员列表、详情和删除 API，前端不调用空实现，也不提供公开注册。题目和竞赛是后续扩展入口。

## 会话与接口约定

- Token 和登录账号保存在当前标签页的 `sessionStorage`，不保存密码。关闭标签页结束本地会话；存储被禁用时仅保存在内存。
- 登录后通过 `GET /sysUser/info` 获取账号与昵称；获取失败则清除本地会话并提示错误。JWT 昵称仅作为展示回退，不用于授予权限。
- 路由守卫只检查是否存在本地 Token；后端通过 JWT 和 Redis 校验真实登录状态。刷新恢复本地会话时不主动请求用户信息，服务端失效由后续受保护请求识别。
- 受保护请求发送 `Authorization: Bearer <token>`；登录请求不带 Token。
- 仅 `code === 1000` 视为成功；HTTP 401 或业务码 3001 清除会话并跳转登录。
- “退出登录”调用 `POST /sysUser/logout`，无需请求体，只需携带当前 Bearer Token。后端先鉴权，再删除 `logintoken:<userKey>`，成功返回 `code: 1000`。前端随后清除本地会话并返回登录页。
- 注销只撤销当前登录会话，不影响同账号的其他会话。未登录、Token 无效或已注销时返回 HTTP 401；鉴权后会话恰好失效、删除返回 false 时返回业务码 3001。
- Redis 删除异常不会返回退出成功；网络或服务错误时，前端保留本地会话并提示重试。HTTP 401 或业务码 3001 仍按登录失效处理。
- 默认开发代理仅用于本地联调，不绕过服务器鉴权。部署时应使用网关统一入口。

## 验证与构建

```powershell
pnpm typecheck
pnpm build
pnpm preview
```

`preview` 仅用于检查构建页面，默认不包含开发代理，真实接口联调使用 `pnpm dev` 或配置下面的生产反向代理。

建议验证：空表单、错误密码、正常登录、刷新恢复、未登录访问受保护页面、重复账号、新增成功、两次密码不一致、登录失效、退出及移动端导航。新增管理员会写入真实数据库，请使用明确的测试账号。

## 部署

将 `dist/` 部署到站点根目录。使用 history 路由，服务器需要将前端路径回退到 `index.html`；`/api` 必须优先代理后端且保持 JSON/HTTP 错误，不能回退到前端 HTML。

Nginx 示例（先验证网关路由）：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:19090/system/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}

location / {
    try_files $uri $uri/ /index.html;
}
```

生产环境在 HTTPS 下部署；可在后端提供完整 Cookie 会话支持后再评估 HttpOnly Cookie 方案。
