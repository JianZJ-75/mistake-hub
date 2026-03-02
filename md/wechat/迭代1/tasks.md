# 微信小程序迭代1 - 任务清单

## Implementation Tasks

- [x] 1. **1.M1 项目初始化**
    - [x] 1.1. 使用 Taro CLI 创建项目
        - *Goal*: 在 `frontend/mistake-hub-wechat` 下创建 Taro 4.x + React 18 + TypeScript 项目
        - *Details*: 在 `frontend/` 目录下执行非交互命令 `npx @tarojs/cli init mistake-hub-wechat --framework react --ts --css sass --compiler webpack5`（⚠️ 禁止使用交互式模式，Bash 工具不支持）。执行 `cd mistake-hub-wechat && npm install`。在 `project.config.json` 中填入 AppID `wxd0173a6f4a4f49c5`。在 `config/dev.ts` 中配置 `defineConstants: { 'process.env.TARO_APP_API': '"http://localhost:8080/mistake-hub/backend"' }`（注意双层引号）。清理默认模板页面，调整目录结构与设计文档一致。验证方式：`npm run dev:weapp` 编译通过
        - *Requirements*: 1.M1
    - [x] 1.2. 创建 storage 工具（集成在 request.ts 中）
        - *Goal*: 封装 token 的存取和清除操作
        - *Details*: 在 `src/utils/request.ts` 中集成 `getToken()` / `setToken(token)` / `removeToken()` 三个方法，使用 `Taro.getStorageSync` / `Taro.setStorageSync` / `Taro.removeStorageSync`，key 为 `mistake_hub_token`
        - *Requirements*: 1.M1
    - [x] 1.3. 创建 request.ts 请求封装
        - *Goal*: 统一请求层，自动注入 token、处理错误、403 自动重登
        - *Details*: 在 `src/utils/request.ts` 中封装 `request<T>()` 方法。请求前从 storage 读取 token 设置 `Authorization: Bearer {token}`。响应处理：HTTP 200 → 解析业务 code → code 为 0 返回 data，非 0 弹 `message.zh_CN` toast；HTTP 403（检查 `res.statusCode`，非 body.code） → 触发重登（clearToken → Taro.login → 直接用 Taro.request 裸调 login-wx → setToken → 重试原请求，最多 1 次，通过 `isRetry` 参数控制）；网络异常 → 弹 toast。定义 `ApiResponse<T>` 接口（`message` 类型为 `{ zh_CN: string; en_US: string }`）。通过 `process.env.TARO_APP_API` 读取 BASE_URL。⚠️ `silentReLogin()` 必须直接用 `Taro.request()` 裸调，禁止 import `account.ts`，避免循环依赖
        - *Requirements*: 1.M1, 1.M2
    - [x] 1.4. 创建 account.ts 接口模块
        - *Goal*: 封装用户相关 API 调用
        - *Details*: 在 `src/service/account.ts` 中实现 `login()` 函数（有 token 时跳过，否则 wx.login → loginWx → setToken）和 `currentDetail()` 方法。定义 `AccountDetailResp` 接口。调用 `request()` 方法请求后端
        - *Requirements*: 1.M1

- [x] 2. **1.M2 静默登录流程**
    - [x] 2.1. 实现 app.tsx 启动登录逻辑
        - *Goal*: 应用启动时自动完成静默登录，全程无 UI
        - *Details*: 在 `src/app.tsx` 中使用 `componentDidMount` 钩子（⚠️ 不是 useEffect，Taro App 类组件生命周期）。调用 `login()` 函数：有 token 则跳过；无 token 则调用 `Taro.login()` 获取 code → POST `/v1/account/login-wx` → `setToken(token)`。登录失败弹 toast 但不阻塞页面渲染
        - *Requirements*: 1.M2

- [x] 3. **1.M3 TabBar 框架**
    - [x] 3.1. 生成 TabBar 图标资源
        - *Goal*: 为 4 个 Tab 程序化生成选中态和未选中态图标
        - *Details*: 图标存放在 `src/assets/tabbar/` 目录下，包含首页、错题、复习、我的四组图标（选中态和未选中态各一张），共 8 张 PNG 图片
        - *Requirements*: 1.M3
    - [x] 3.2. 配置 app.config.ts
        - *Goal*: 注册页面路由和 TabBar 导航
        - *Details*: 在 `src/app.config.ts` 中配置 `pages` 数组（index / mistake / review / profile）和 `tabBar` 对象（color / selectedColor / backgroundColor / borderStyle / list），list 中 4 项对应 4 个 Tab 页及其图标路径
        - *Requirements*: 1.M3
    - [x] 3.3. 创建 4 个 Tab 占位页面
        - *Goal*: 每个 Tab 有可渲染的占位页面
        - *Details*: 分别创建 `pages/index/index.tsx`、`pages/mistake/index.tsx`、`pages/review/index.tsx`、`pages/profile/index.tsx`，每个页面居中显示功能说明文字，后续迭代填充实际内容。同步创建对应 `index.config.ts`（设置 navigationBarTitleText）和 `index.scss`
        - *Requirements*: 1.M3

- [x] 4. **1.M4 修复请求层**
    - [x] 4.1. 修复 message 类型定义
        - *Goal*: `ApiResponse` 中 `message` 字段类型与后端 `BaseResult` 严格对齐
        - *Details*: 将 `message` 字段类型从 `string` 改为 `{ zh_CN: string; en_US: string }`，与后端双语消息结构一致，避免运行时类型错误
        - *Requirements*: 1.M4
    - [x] 4.2. 修复 403 处理逻辑
        - *Goal*: 403 时自动重登并重试原请求，最多重试 1 次
        - *Details*: 检查 `res.statusCode === 403`（HTTP 状态码，非 body.code），调用 `silentReLogin()` 完成重登后重试原请求。`silentReLogin()` 直接使用 `Taro.request` 裸调登录接口，不调用 `account.ts` 的方法，通过 `isRetry` 参数防止无限循环
        - *Requirements*: 1.M4

## Task Dependencies

- 任务 1.1（创建项目）必须最先完成，所有后续任务依赖项目骨架
- 任务 1.2（storage，集成在 request.ts）必须在 1.3（request.ts）之前设计确认
- 任务 1.3（request.ts）必须在 1.4（account.ts）之前完成，account 依赖 request
- 任务 1.4（account.ts）必须在 2.1（app.tsx 登录）之前完成，登录依赖 loginWx 方法
- 任务 3.1（图标资源）和 3.2（app.config.ts）可与任务 1.2~1.4 并行
- 任务 3.3（占位页面）依赖 3.2（路由配置）完成
- 任务 4（修复请求层）为独立修复，可在 1.3 完成后任意时机进行

```
1.1 → 1.2 → 1.3 → 1.4 → 2.1
1.1 → 3.1 → 3.2 → 3.3
1.3 → 4（修复）
```
