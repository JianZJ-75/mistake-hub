# Architecture

## 项目简介

**错题复习调度系统**（Mistake Hub）：基于艾宾浩斯遗忘曲线，帮助学生自动调度错题复习。包含后端 API、Web 管理端、微信小程序三个端。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.4, MyBatis Plus 3.5, MySQL 8, Redis + Redisson |
| Web 管理端 | React 19, Vite, TypeScript, Tailwind CSS, shadcn/ui, Zustand, axios |
| 微信小程序 | Taro 4, React, TypeScript, SCSS |
| 基础设施 | 阿里云 OSS（图片）, springdoc-openapi（Swagger） |

---

## Maven 多模块

```
mistake-hub-all (pom)
├── dependencies/    BOM 统一管理第三方依赖版本
├── common/          公共基础设施（BaseResult, BaseException, oops(), ThreadStorageUtil, 分布式锁）
└── backend/         业务主模块（Controller → Service → Mapper）
```

依赖方向：`backend → common → dependencies`

---

## 后端包结构

```
com.jianzj.mistake.hub.backend
├── annotation/       自定义注解（@PreAuthorize）
├── config/mvc/       MVC 配置（RequestAuthInterceptor, CustomizedWebMvcConfig）
├── controller/       REST 控制器
│   ├── AccountController     /v1/account/*
│   ├── MistakeController     /v1/mistake/*
│   ├── TagController         /v1/tag/*
│   ├── UploadController      /v1/upload/*
│   ├── NonceController       /v1/nonce/*
│   └── SessionController     /v1/session/*
├── dto/
│   ├── req/          请求对象（XxxReq）
│   └── resp/         响应对象（XxxResp）
├── entity/           数据库实体（Account, Mistake, Tag, MistakeTag, Session, Nonce）
├── enums/            枚举（Role, TagType）
├── mapper/           MyBatis Plus Mapper 接口
├── service/          业务逻辑层（直接继承 ServiceImpl，无 IService 接口）
└── utils/encryption/ 加解密工具（AesUtil, EncryptionUtil）
```

### common 公共模块包结构

```
com.jianzj.mistake.hub.common
├── base/             ApplicationContextHolder, FastJsonSafeMode
├── convention/
│   ├── exception/    BaseException (oops()), ApiAuthorizeException
│   ├── result/       BaseResult 统一响应体
│   └── enums/        BasicEnumPojo
├── lock/             分布式锁基础设施
│   ├── annotation/   @DistributedLockAnno
│   ├── aspect/       AOP 切面
│   ├── config/       条件配置（MySQL / Redis 自动切换）
│   ├── entity/       DistributedLock 实体
│   ├── mapper/       DistributedLockMapper
│   ├── service/      LockService 接口 + DistributedLockService
│   │   └── impl/     抽象模板 + MySQL/Redis 实现
│   └── support/      SpEL 表达式解析器
└── utils/            ThreadStorageUtil
```

---

## 前端结构

### Web 管理端 (`frontend/mistake-hub-web`)

```
src/
├── api/
│   ├── apiClient.ts              axios 封装（token 注入, 403 跳转, BaseResult 解包）
│   └── services/                 按业务拆分的 API 调用
│       ├── userService.ts        用户相关（含加密逻辑）
│       ├── mistakeService.ts     错题 CRUD
│       ├── tagService.ts         标签 CRUD
│       ├── uploadService.ts      图片上传
│       └── nonceService.ts       Nonce 生成
├── pages/management/             业务页面
│   ├── user/index.tsx            用户管理
│   ├── mistake/index.tsx         错题管理
│   └── tag/index.tsx             标签管理
├── store/userStore.ts            Zustand 状态管理
├── types/entity.ts               与后端 DTO 对应的 TypeScript 类型
├── utils/crypto.ts               AES-256-CBC 加解密（与后端 AesUtil 对齐）
├── routes/                       路由配置
├── layouts/                      布局组件（复用模板，勿改）
└── ui/                           shadcn/ui 组件
```

### 微信小程序 (`frontend/mistake-hub-wechat`)

```
src/
├── app.tsx                       入口（静默登录）
├── pages/
│   ├── mistake/index.tsx         错题列表（筛选 + 分页 + 下拉刷新）
│   ├── mistake-add/index.tsx     错题录入
│   ├── mistake-detail/index.tsx  错题详情 / 编辑
│   ├── review/index.tsx          复习页（占位，迭代 4 填充）
│   └── profile/index.tsx         个人设置（占位，迭代 5 填充）
├── service/                      API 调用层
├── utils/
│   ├── request.ts                Taro.request 封装（403 重登 + 重试）
│   └── mistake.ts                公共工具函数（flattenTags, getMasteryInfo）
├── styles/                       公共样式
└── types/                        类型定义
```

---

## 数据库表

| 迭代 | 表名 | 说明 |
|:---:|------|------|
| 1 | `account` | 用户（code, role, wechat_open_id, daily_limit） |
| 1 | `session` | 会话（token, expire_time） |
| 1 | `nonce` | 一次性随机数（AES 密钥交换） |
| 2 | `mistake` | 错题（title, review_stage, mastery_level, next_review_time, status 软删除） |
| 2 | `tag` | 标签树（name, type: SUBJECT/CHAPTER/KNOWLEDGE, parent_id） |
| 2 | `mistake_tag` | 错题-标签多对多关联 |
| infra | `distributed_lock` | 分布式锁（lock_name 唯一约束, expired_time 自动过期） |
| 3 | `review_plan` | 复习计划（planned_date, status: PENDING/COMPLETED/SKIPPED） |
| 3 | `review_record` | 复习记录（is_correct, stage/mastery before/after 快照） |
| 6 | `system_config` | 系统配置键值对 |
| 6 | `operation_log` | 操作日志 |

DDL 定义见 `resources/db/mistake-hub.sql`。

---

## 核心业务流程

### 认证

```
微信小程序: wx.login() → code → POST /v1/account/login-wx → token
Web 管理端: 密码 → SHA256 → AES(nonce) → POST /v1/account/login-web → token
所有请求: Header Authorization: {token} → RequestAuthInterceptor 校验
```

### 错题生命周期

```
录入 → review_stage=0, mastery=0, next_review_time=now
  → 定时任务每日生成 review_plan
  → 学生复习作答
  → 答对: stage+1, mastery+20  /  答错: stage-2, mastery-15
  → 更新 next_review_time = now + INTERVALS[new_stage]
```

### 艾宾浩斯间隔

```
Stage:    0    1    2    3    4    5    6
Days:     0    1    2    4    7   15   30
Priority: overdue_days×3 + (100-mastery)×0.5 + (6-stage)×2
```

---

## 启动命令

```bash
# 后端（端口 18080, Swagger: http://localhost:18080/swagger-ui.html）
cd dependencies && mvn install -Dmaven.test.skip=true -q && cd ..
cd backend && mvn spring-boot:run -Dmaven.test.skip=true

# Web 管理端（端口 5173）
cd frontend/mistake-hub-web && pnpm dev

# 微信小程序（生成到 dist/，微信开发者工具打开）
cd frontend/mistake-hub-wechat && npm run dev:weapp
```

---

## 迭代进度

| 迭代 | 内容 | 状态 |
|:---:|------|:---:|
| 1 | 认证体系 + 用户管理 | ✅ |
| 2 | 错题数据模型 + 采集管理 + 标签体系 | ✅ |
| 3 | 艾宾浩斯复习调度算法（纯后端） | 待开始 |
| 4 | 复习执行与结果反馈 | 待开始 |
| 5 | 学习数据统计与可视化 | 待开始 |
| 6 | 管理端增强 + 系统配置 | 待开始 |
| 6.5 | 全局代码质量优化 | 待开始 |
| 7 | 系统测试 | 待开始 |

详细任务清单见 `md/实施规划.md`，编码规范见 `CLAUDE.md`。
