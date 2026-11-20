# Apifox 用户管理接口上手清单

本说明对应当前项目已实现的登录、新增管理员接口。文件位于 `D:/code/ideaPrograms/java_steins_gate/java_code/Spring/amadeus-oj/docs/apifox/`。

## 已准备的内容

- 已在当前 Apifox 的 `amadeus-oj` 项目保存环境“本地直连-用户管理”，并创建下表 8 个变量。已有管理员账号、密码留空，等待你在本地填写。
- `sys-user.postman_collection.json`：Postman Collection v2.1 格式，含 9 个请求、前置检查、业务码断言、登录后自动保存 JWT，以及新管理员 JWT 昵称检查。
- `direct.postman_environment.json`：直连环境备份。
- `gateway.postman_environment.json`：网关环境模板，地址沿用当前项目原登录请求的配置，具体路由仍需与配置中心核对。
- `environment-local.csv`：Apifox“环境变量 → 批量编辑”使用的内容。新建环境可以使用；不要直接覆盖已有环境的变量列表。

本轮尚未完成集合在 Apifox 客户端里的导入：操作期间窗口被最小化，恢复时工具提示检测到用户输入。按以下步骤导入即可。

## 1. 启动后端并选择环境

- [ ] 在 IDEA 启动 `com.amadeus.system.OjSystemApplication`，确认启动日志显示端口 `9201`。
- [ ] 确认应用使用的 MySQL、Redis 和配置中心可连接。上轮集成测试使用的临时容器已清理，测试通过不代表日常开发服务仍在运行。
- [ ] Apifox 右上角选择“本地直连-用户管理”，使用真实服务环境。
- [ ] 如果实际端口不是 `9201`，同时调整前置 URL 和 `base_url` 本地值。导入请求实际使用的是 `base_url`。

| 访问方式 | base_url | 登录完整 URL | 新增完整 URL |
| --- | --- | --- | --- |
| 系统服务直连 | `http://localhost:9201` | `http://localhost:9201/sysUser/login` | `http://localhost:9201/sysUser/add` |
| 网关（待确认路由） | `http://localhost:19090/system` | `http://localhost:19090/system/sysUser/login` | `http://localhost:19090/system/sysUser/add` |

网关监听端口在仓库配置为 `19090`；`/system` 前缀来自当前 Apifox 原登录请求。经网关测试需要同时启动网关和系统服务，并确认配置中心有 `/system/**` 路由、正确的去前缀规则，以及登录白名单。初次联调建议先使用直连环境。

## 2. 填写环境变量的本地值

打开右上角环境下拉框 → 管理环境 → 本地直连-用户管理。

| 变量 | 已配置的本地值 | 作用 |
| --- | --- | --- |
| `base_url` | `http://localhost:9201` | 请求地址前缀，无末尾 `/` |
| `admin_account` | 留空，填写已有管理员账号 | 第 01 个请求使用 |
| `admin_password` | 留空，填写已有管理员密码 | 第 01 个请求使用 |
| `token` | 留空，由第 01 个请求自动保存 | 新增请求的管理员 JWT |
| `new_account` | `apifox_admin_demo_01` | 本轮要新增的测试管理员账号 |
| `new_password` | `ApifoxDemo_2026!` | 本轮新增测试账号的示例密码 |
| `new_nickname` | `Apifox测试管理员` | 本轮新增昵称，支持中文 |
| `new_token` | 留空，由第 03 个请求自动保存 | 新账号的 JWT，和原管理员 Token 分开保存 |

- [ ] 填写 `admin_account`、`admin_password` 的**本地值**，保存。
- [ ] 本轮测试前确认 `new_account` 尚未存在；再次运行整组请求时换一个新账号名。
- [ ] `token` 和 `new_token` 保存登录响应 `data` 的完整字符串；不要保存 `userKey` 或只有一段 UUID 的值。
- [ ] Token 变量中不加 `Bearer `；新增请求已经配置 `Authorization: Bearer {{token}}`。

若数据库暂时没有任何可登录管理员，需要先准备初始管理员；当前没有专门的初始化接口。已有账号的昵称为空时，可用于第 01 步登录，但昵称验证使用的是第 02 步新建的账号。

## 3. 导入这组请求

- [ ] 在当前项目进入 **项目设置 → 数据管理 → 导入数据 → Postman**。
- [ ] 选择文件 `D:/code/ideaPrograms/java_steins_gate/java_code/Spring/amadeus-oj/docs/apifox/sys-user.postman_collection.json`，进行预览。
- [ ] 导入到独立目录“用户管理联调（按编号执行）”，保留请求示例、前置脚本和后置脚本。
- [ ] 集合只有两个接口路径，多条请求用于不同测试场景。如果导入器按方法、路径合并接口，选择保留为接口用例；先查看预览，确保 9 个场景都保留。若版本提供“导入为 HTTP 请求/快捷请求”，也可选择该方式保留各请求。
- [ ] 导入后展开目录，检查登录请求的后置操作中有 `pm.environment.set(...)`；新增请求的 Headers 中有 `Authorization`。
- [ ] 选择刚才的直连环境。环境已经在客户端配置，无须重复导入直连环境备份文件。

如果版本的菜单布局不同，可用“导入数据”入口选择 Postman 格式。环境备份文件是 Postman Environment 格式，需要在支持环境导入的位置单独选择，不要当成接口集合导入。

## 4. 先跑通这三个请求

### 01 已有管理员登录

```text
POST {{base_url}}/sysUser/login
Content-Type: application/json
```

Body 选择 **JSON**：

```json
{
  "userAccount": "{{admin_account}}",
  "password": "{{admin_password}}"
}
```

发送后检查 HTTP 状态，以及响应体：

```json
{
  "code": 1000,
  "msg": "操作成功",
  "data": "eyJ...完整的JWT..."
}
```

后置脚本已配置：只有业务成功且返回三段式 JWT 时，才将 `data` 保存到环境变量 `token`；失败时清空该变量，避免沿用旧 Token。

### 02 新增管理员

```text
POST {{base_url}}/sysUser/add
Content-Type: application/json
Authorization: Bearer {{token}}
```

```json
{
  "userAccount": "{{new_account}}",
  "password": "{{new_password}}",
  "nickName": "{{new_nickname}}"
}
```

字段名是 **`nickName`**，请求中大小写应与 DTO 一致。预期 `code = 1000`，`data = null`。新增接口不会返回 Token；下一步通过登录取得。

### 03 新管理员登录

```text
POST {{base_url}}/sysUser/login
```

```json
{
  "userAccount": "{{new_account}}",
  "password": "{{new_password}}"
}
```

预期 `code = 1000`，后置脚本保存 `new_token`，并解码 JWT 的 Payload，断言 `nickName` 与环境中的 `new_nickname` 一致。中文解码使用 UTF-8。

这项客户端断言只验证返回内容。JWT 签名有效性、Redis 登录态仍应由后端验证；二者已由前一轮集成测试覆盖。JWT 中的昵称是签发时的快照，已有 Token 不会自动获得后来修改的昵称。

## 5. 按清单检查异常场景

本项目业务码位于响应 JSON 的 `code` 字段；HTTP `200` 不代表业务操作成功。

| 场景 | 请求 | 预期业务码 | 运行前提 |
| --- | --- | --- | --- |
| 01 已有管理员登录 | `POST /sysUser/login` | 1000 | 本地环境填写有效账号密码 |
| 02 新增管理员 | `POST /sysUser/add` | 1000 | 已取得管理员 Token，测试账号尚不存在 |
| 03 新管理员登录 | `POST /sysUser/login` | 1000 | 02 成功，昵称断言通过 |
| 04 重复新增 | `POST /sysUser/add` | 3101 | 使用 02 已创建的相同账号 |
| 05 缺少 nickName | `POST /sysUser/add` | 3002 | 已登录 |
| 06 nickName 全空格 | `POST /sysUser/add` | 3002 | 已登录 |
| 07 密码错误 | `POST /sysUser/login` | 3103 | 02 已创建账号 |
| 08 账号不存在 | `POST /sysUser/login` | 3102 | `_not_exists` 后缀账号未被创建 |
| 09 JSON 格式错误 | `POST /sysUser/add` | 3002 | 已登录 |

- [ ] 每个请求点击“发送”，同时查看**响应体**和**断言结果**。
- [ ] 04 返回 3101 是测试通过；02 返回 3101 表示账号已存在，应改 `new_account` 或从 03 继续验证。
- [ ] 手动修改请求时点“保存”，把可复现的请求保存为接口用例。
- [ ] 第一轮手动跑通后，再在“自动化测试”创建测试场景，按 01 → 09 添加已保存的请求/接口用例，选择同一环境后运行。
- [ ] 批量重跑前修改 `new_account`。本组正常新增会保留测试账号，不提供自动删除步骤。

当前 `GET /sysUser/detail`、`DELETE /sysUser/{userId}` 的 Controller 仍返回 `null`，不适合用来验证昵称查询、清理测试账号或判断业务成功。

新增请求已带管理员 Token，但这本身不能证明后端鉴权已生效。如果后续要做无 Token/普通用户 Token 的权限测试，应分别去掉 Header、替换 Token，并检查实际拦截器和网关注册情况。

## 6. 常见问题按这个顺序排查

| 现象 | 优先检查 |
| --- | --- |
| 连接被拒绝、ECONNREFUSED | IDEA 服务是否启动、端口是否为 9201；切换网关时网关是否启动 |
| 404 | 是否混用 9201 与 `/system` 前缀；网关路由是否正确 |
| 405 | 是否把 POST 请求配置成 GET |
| 3001 或 HTTP 401 | 是否先登录、Token 是否完整、是否过期、是否切换了环境 |
| 3002 | Body 是否为 JSON；`userAccount`、`password`、`nickName` 是否非空白；字段是否写成了 `nickname` |
| 3101 | 新增账号已存在，修改 `new_account` 后重新开始 |
| 3102/3103 | 登录账号是否存在、账号密码是否与当前数据库对应 |
| 2000 | 查看服务端异常日志，检查数据库和 Redis 连接；不要只重发请求 |
| `{{变量}}` 未替换 | 当前是否选中正确环境、变量名拼写和本地值是否正确 |
| 脚本提示先填写账号或先登录 | 在当前环境填本地值，按 01 → 02 → 03 顺序运行 |
| 看不到自动保存的 Token | 检查后置脚本是否导入、业务码是否 1000、查看的是不是同一环境 |

## 7. Apifox 后续学习清单

| 功能 | 现在怎么用 |
| --- | --- |
| 接口调试 | 配方法、URL、Headers、JSON Body 后发送请求 |
| 环境与变量 | 通过 `{{base_url}}` 切换直连/网关，通过本地变量保存当前登录信息 |
| 接口设计与文档 | 给字段写类型、必填项、示例和业务码，维护前后端共同契约 |
| 接口用例 | 保存“正常新增”“缺少昵称”等不同输入和断言，方便复现 |
| 前置/后置操作 | 发送前检查变量，响应后提取 Token、添加业务断言 |
| 自动化测试 | 按顺序组合登录、新增和异常场景，查看整组通过率 |
| Mock | 在后端未完成时按文档生成模拟响应；Mock 结果不能证明真实数据库或认证逻辑正常 |
| 文档共享与协作 | 与前端共享字段、示例和状态码；先让已验证的接口文档保持准确 |
| 导入导出 | 用 OpenAPI、Postman 等格式迁移接口；本目录文件可作为本轮配置备份 |

建议学习顺序：调试 → 环境变量 → 保存接口用例 → 后置断言 → 自动化测试 → 文档与 Mock。
