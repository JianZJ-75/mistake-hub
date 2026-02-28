# 微信小程序迭代1 - 任务清单

## Implementation Tasks

- [ ] 1. **1.M1 项目初始化**
    - [ ] 1.1. 使用 Taro CLI 创建项目
        - *Goal*: 在 `frontend/mistake-hub-wechat` 下创建 Taro 4.x + React 18 + TypeScript 项目
        - *Details*: 在 `frontend/` 目录下执行非交互命令 `npx @tarojs/cli init mistake-hub-wechat --framework react --ts --css sass --compiler webpack5`（⚠️ 禁止使用交互式模式，Bash 工具不支持）。执行 `cd mistake-hub-wechat && npm install`。在 `project.config.json` 中填入 AppID `wxd0173a6f4a4f49c5`。在 `config/dev.ts` 中配置 `defineConstants: { 'process.env.TARO_APP_API': '"http://localhost:8080/mistake-hub/backend"' }`（注意双层引号）。清理默认模板页面，调整目录结构与设计文档一致。验证方式：`npm run dev:weapp` 编译通过
        - *Requirements*: 1.M1
    - [ ] 1.2. 创建 storage.ts 工具
        - *Goal*: 封装 token 的存取和清除操作
        - *Details*: 在 `src/utils/storage.ts` 中实现 `getToken()` / `setToken(token)` / `clearToken()` 三个方法，使用 `Taro.getStorageSync` / `Taro.setStorageSync` / `Taro.removeStorageSync`，key 为 `mistake_hub_token`
        - *Requirements*: 1.M1
    - [ ] 1.3. 创建 request.ts 请求封装
        - *Goal*: 统一请求层，自动注入 token、处理错误、403 自动重登
        - *Details*: 在 `src/api/request.ts` 中封装 `request<T>()` 和 `post<T>()` 方法。请求前从 storage 读取 token 设置 `Authorization: Bearer {token}`。响应处理：HTTP 200 → 解析业务 code → code 为 0 返回 data，非 0 弹 `message.zh_CN` toast；HTTP 403 → 触发重登（clearToken → Taro.login → 直接用 Taro.request 裸调 login-wx → setToken → 重试原请求，最多 1 次）；网络异常 → 弹 toast。定义 `ApiResponse<T>` 接口。⚠️ 403 重登必须直接用 `Taro.request()` 裸调，禁止 import `account.ts`，避免循环依赖。通过 `process.env.TARO_APP_API` 读取 BASE_URL
        - *Requirements*: 1.M1, 1.M2
    - [ ] 1.4. 创建 account.ts 接口模块
        - *Goal*: 封装用户相关 API 调用
        - *Details*: 在 `src/api/account.ts` 中实现 `loginWx(code: string): Promise<string>` 和 `currentDetail(): Promise<AccountDetailResp>`。定义 `AccountDetailResp` 接口。调用 `post()` 方法请求后端
        - *Requirements*: 1.M1

- [ ] 2. **1.M2 静默登录流程**
    - [ ] 2.1. 实现 app.ts 启动登录逻辑
        - *Goal*: 应用启动时自动完成静默登录，全程无 UI
        - *Details*: 在 `src/app.ts` 中使用 Taro 专有的 `useLaunch` 钩子（⚠️ 不是 useEffect，Taro App 组件不支持 useEffect 做启动逻辑）。检查本地 token：有 token 则跳过；无 token 则调用 `Taro.login()` 获取 code → `loginWx(code)` → `setToken(token)`。App 组件接收 `PropsWithChildren`，return children。登录失败弹 toast 但不阻塞页面渲染。参考设计文档 app.ts 完整示例代码
        - *Requirements*: 1.M2

- [ ] 3. **1.M3 TabBar 框架**
    - [ ] 3.1. 生成 TabBar 图标资源
        - *Goal*: 为 4 个 Tab 程序化生成选中态和未选中态图标
        - *Details*: 安装 `@napi-rs/canvas` 为 devDependency。编写 `scripts/generate-icons.js` 脚本，使用 canvas 绘制 4 组图标（首页=房子轮廓、错题=文档+×号、复习=循环箭头、我的=人形轮廓），每组生成未选中态（`#999999`）和选中态（`#1890ff`）共 8 张 81×81 PNG。输出到 `src/assets/tabbar/`。执行 `node scripts/generate-icons.js` 生成
        - *Requirements*: 1.M3
    - [ ] 3.2. 配置 app.config.ts
        - *Goal*: 注册页面路由和 TabBar 导航
        - *Details*: 在 `src/app.config.ts` 中配置 `pages` 数组（home / mistakes / review / mine）和 `tabBar` 对象（color / selectedColor / backgroundColor / borderStyle / list），list 中 4 项对应 4 个 Tab 页及其图标路径
        - *Requirements*: 1.M3
    - [ ] 3.3. 创建 4 个 Tab 占位页面
        - *Goal*: 每个 Tab 有可渲染的占位页面
        - *Details*: 分别创建 `pages/home/index.tsx`、`pages/mistakes/index.tsx`、`pages/review/index.tsx`、`pages/mine/index.tsx`，每个页面结构相同：居中显示页面标题文字。同步创建 `index.config.ts`（设置 navigationBarTitleText）和 `index.scss`
        - *Requirements*: 1.M3

## Task Dependencies

- 任务 1.1（创建项目）必须最先完成，所有后续任务依赖项目骨架
- 任务 1.2（storage.ts）必须在 1.3（request.ts）之前完成，request 依赖 token 存取
- 任务 1.3（request.ts）必须在 1.4（account.ts）之前完成，account 依赖 request
- 任务 1.4（account.ts）必须在 2.1（app.ts 登录）之前完成，登录依赖 loginWx 方法
- 任务 3.1（图标资源）和 3.2（app.config.ts）可与任务 1.2~1.4 并行
- 任务 3.3（占位页面）依赖 3.2（路由配置）完成

```
1.1 → 1.2 → 1.3 → 1.4 → 2.1
1.1 → 3.1 → 3.2 → 3.3
```

## Estimated Timeline

- 任务 1.1（项目创建）: 15 分钟
- 任务 1.2（storage.ts）: 5 分钟
- 任务 1.3（request.ts）: 20 分钟
- 任务 1.4（account.ts）: 10 分钟
- 任务 2.1（app.ts 登录）: 10 分钟
- 任务 3.1（图标资源）: 10 分钟
- 任务 3.2（app.config.ts）: 5 分钟
- 任务 3.3（占位页面）: 10 分钟
- **Total: ~85 分钟**
