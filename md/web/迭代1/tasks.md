# Web 管理端迭代1 - 任务清单

## Implementation Tasks

- [x] 1. **1.W1 项目初始化**
    - [x] 1.1. 使用 Vite 创建项目
        - *Goal*: 在 `frontend/mistake-hub-web` 下创建 React 18 + TypeScript 项目
        - *Details*: 在 `frontend/` 目录下执行 `npm create vite@latest mistake-hub-web -- --template react-ts`，进入目录执行 `npm install`，再安装依赖 `npm install antd @ant-design/icons axios crypto-js react-router-dom` 和 `npm install -D @types/crypto-js`。创建 `.env.development`（`VITE_API_BASE_URL=http://localhost:8080/mistake-hub/backend`）和 `.env.production`。清理默认模板文件，调整目录结构与设计文档一致。验证方式：`npm run dev` 启动无报错
        - *Requirements*: 1.W1
    - [x] 1.2. 创建 storage.ts 工具
        - *Goal*: 封装 token 的 localStorage 存取和清除操作
        - *Details*: 在 `src/utils/storage.ts` 中实现 `getToken()` / `setToken(token)` / `clearToken()` 三个方法，使用 `localStorage.getItem` / `setItem` / `removeItem`，key 为 `mistake_hub_token`
        - *Requirements*: 1.W1
    - [x] 1.3. 创建 crypto.ts 加密工具
        - *Goal*: 封装与后端 AesUtil 对齐的 SHA-256 和 AES-256-CBC 加解密方法
        - *Details*: 在 `src/utils/crypto.ts` 中实现 `sha256()`、`aesEncrypt(plainText, rawKey)`、`aesDecrypt(cipherTextWithIv, rawKey)`、`aesDecryptDirect(cipherTextWithIv, key)` 四个方法。使用 Web Crypto API（零依赖）。密钥派生：`sha256(rawKey).substring(0,32)`。IV：16 字节随机，Base64 编码。输出格式：`cipherBase64_ivBase64`（下划线分隔，对应后端 `IV_SEPARATOR = "_"`）。`aesDecryptDirect` 直接使用 key（无 SHA256 派生），用于解密后端加密返回的密码
        - *Requirements*: 1.W1, 1.W2
    - [x] 1.4. 创建 apiClient.ts axios 封装
        - *Goal*: 统一请求层，自动注入 token、处理错误、403 跳转登录页
        - *Details*: 在 `src/api/apiClient.ts` 中创建 axios 实例（baseURL 读 `import.meta.env.VITE_API_BASE_URL`，timeout 10000）。请求拦截器：从 Zustand store 读取 token 设置 `Authorization: Bearer {token}`。响应拦截器：HTTP 200 + code === 0 → 返回 data；HTTP 200 + code !== 0 → sonner `toast.error` → reject；HTTP 403 → 清 token → `window.location.href = '/auth/login'` → reject；网络异常 → `toast.error`
        - *Requirements*: 1.W1
    - [x] 1.5. 创建 nonceService.ts 和 userService.ts 接口模块
        - *Goal*: 封装 Nonce 和用户相关 API 调用
        - *Details*: `src/api/services/nonceService.ts` 实现 `generateNonce(): Promise<NonceResp>` 和 `getNonce(id): Promise<NonceResp>`，定义 `NonceResp { id: number, nonce: string }`。`src/api/services/userService.ts` 实现 `loginWeb`、`currentDetail`、`changePassword`、`resetPassword`、`changeRole`、`listUsers`、`updateDailyLimit` 等方法，内部封装 Nonce 获取和加解密逻辑
        - *Requirements*: 1.W1

- [x] 2. **1.W2 登录页**
    - [x] 2.1. 创建路由配置和路由守卫
        - *Goal*: 配置 react-router v7 路由，实现未登录自动跳转
        - *Details*: 在 `src/routes/` 中配置路由：`/auth/login` → 登录页；`/` → 路由守卫包裹的 DashboardLayout → 子路由。路由守卫：token 有值则 `<Outlet />`，无值则 `<Navigate to="/auth/login" replace />`。在 `src/main.tsx` 中用 `RouterProvider` 挂载路由
        - *Requirements*: 1.W1, 1.W2
    - [x] 2.2. 实现登录页
        - *Goal*: 完整的登录表单，集成 Nonce 加密流程
        - *Details*: 在 `src/pages/sys/login/` 中实现登录页。字段：用户名（code）+ 密码（支持显示/隐藏）。提交逻辑：1) `generateNonce()` 获取 `{id, nonce}`；2) `sha256(password)` 得到 digest；3) `aesEncrypt(digest, nonce)` 得到 cipherDigest；4) POST `/v1/account/login-web` 得到 token；5) 存 token 到 Zustand store；6) navigate('/')。已有 token 且访问登录页时重定向首页
        - *Requirements*: 1.W2

- [x] 3. **1.W3 布局框架**
    - [x] 3.1. 实现 DashboardLayout 布局组件
        - *Goal*: 带侧边栏和顶部栏的管理端布局框架，基于 slash-admin 模板
        - *Details*: 复用 slash-admin 模板已有 `src/layouts/dashboard/` 布局，包含可折叠侧边栏、顶部导航栏（展示当前用户 + 退出登录下拉菜单）。菜单配置在 `src/layouts/dashboard/nav/nav-data/nav-data-frontend.tsx` 中，配置「工作台」和「用户管理」两个菜单项
        - *Requirements*: 1.W3
    - [x] 3.2. 创建首页占位页面
        - *Goal*: 首页 Tab 有可渲染的占位内容
        - *Details*: 在 `src/pages/dashboard/workbench/` 中创建首页占位组件，后续迭代填充实际内容
        - *Requirements*: 1.W3

- [x] 4. **1.W4 修改密码**
    - [x] 4.1. 顶部栏下拉菜单入口
        - *Goal*: 在顶部栏用户下拉菜单中提供「修改密码」入口，弹出 Dialog
        - *Details*: 在 `src/layouts/components/account-dropdown.tsx` 中实现下拉菜单，包含「修改密码」和「退出登录」选项。点击「修改密码」弹出 Dialog，包含旧密码、新密码、确认新密码三个字段（shadcn/ui Dialog + Input）
        - *Requirements*: 1.W4
    - [x] 4.2. 修改密码加密逻辑
        - *Goal*: 双 Nonce + AES 加密，修改成功后强制退出登录
        - *Details*: `userService.changePassword()` 同时获取两个 Nonce，分别对旧密码（SHA-256 + AES）和新密码（SHA-256 + AES）加密后发送。修改成功后 `clearToken()` → navigate('/auth/login')
        - *Requirements*: 1.W4

- [x] 5. **1.W5 用户管理页（重置密码）**
    - [x] 5.1. 用户列表分页查询
        - *Goal*: 按 code / 昵称 / 角色筛选用户，分页展示
        - *Details*: 在 `src/pages/management/user/index.tsx` 中实现用户管理页，调用 `userService.listUsers()` 分页查询，支持按 role 筛选，使用 shadcn/ui Table 展示
        - *Requirements*: 1.W5
    - [x] 5.2. 重置密码并解密展示
        - *Goal*: 管理员可重置指定用户密码，前端解密展示明文
        - *Details*: 管理员用户行显示「重置密码」按钮，调用 `userService.resetPassword(accountId)` → 后端返回 `{nonceId, cipherPassword}` → 前端 `nonceService.getNonce(nonceId)` 获取 nonce → `aesDecryptDirect(cipherPassword, nonce)` 解密 → 弹窗展示明文密码，支持一键复制
        - *Requirements*: 1.W5

- [x] 6. **1.W6 用户管理页（升级/降级角色）**
    - [x] 6.1. 升级为管理员（含密码解密展示）
        - *Goal*: 管理员可将学生升级为管理员，前端提供 nonce，解密后端返回的加密密码并展示
        - *Details*: 学生用户行显示「升级」按钮，调用 `userService.changeRole(accountId, "admin")` → 内部先获取 Nonce → 后端使用该 Nonce 加密生成的密码返回 `{cipherPassword, nonceId}` → 前端 `aesDecryptDirect()` 解密 → 弹窗展示明文密码
        - *Requirements*: 1.W6
    - [x] 6.2. 降级为学生
        - *Goal*: 管理员可将管理员账号降级为学生，清除密码强制下线
        - *Details*: 管理员用户行显示「降级」按钮，二次确认后调用 `userService.changeRole(accountId, "student")`，后端清除密码并踢出 Session
        - *Requirements*: 1.W6

## Task Dependencies

- 任务 1.1（创建项目）必须最先完成，所有后续任务依赖项目骨架
- 任务 1.2（storage.ts）必须在 1.4（apiClient.ts）之前完成，request 依赖 token 存取
- 任务 1.3（crypto.ts）必须在 2.2（登录页）之前完成，登录依赖加密
- 任务 1.4（apiClient.ts）必须在 1.5（service 模块）之前完成，接口依赖 post 方法
- 任务 1.5（API 模块）必须在 2.2（登录页）之前完成，登录页依赖接口
- 任务 2.1（路由配置）可与 1.3~1.5 并行
- 任务 3.1（DashboardLayout）依赖 2.1（路由配置）和 1.5（userService）
- 任务 4~6 依赖 3.1（布局框架）和 1.5（userService）

```
1.1 → 1.2 → 1.4 → 1.5 → 2.2
1.1 → 1.3 → 2.2
1.1 → 2.1 → 3.1 → 3.2
3.1 → 4, 5, 6
```
