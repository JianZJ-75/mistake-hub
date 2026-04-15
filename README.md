# Mistake Hub - 错题复习调度系统

基于**艾宾浩斯遗忘曲线**的智能错题复习调度系统。学生通过微信小程序录入错题，系统自动生成个性化复习计划；管理员通过 Web 端管理用户、标签和系统配置。

## 技术栈

| 端 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.4, MyBatis Plus 3.5, MySQL 8, Redis + Redisson |
| Web 管理端 | React 19, Vite, TypeScript, Tailwind CSS, shadcn/ui, Zustand |
| 微信小程序 | Taro 4, React, TypeScript, SCSS |
| 基础设施 | 阿里云 OSS（图片存储）, springdoc-openapi（API 文档） |

## 项目结构

```
mistake-hub/
├── dependencies/                    # Maven BOM，统一管理第三方依赖版本
├── common/                          # 公共基础设施（BaseResult, BaseException, 分布式锁）
├── backend/                         # 业务主模块（Controller → Service → Mapper）
├── frontend/
│   ├── mistake-hub-web/             # Web 管理端
│   └── mistake-hub-wechat/          # 微信小程序
├── resources/db/                    # 数据库脚本（DDL + 测试数据）
├── docs/                            # 项目文档
├── docker-compose.yml               # MySQL + Redis 一键启动
└── ARCHITECTURE.md                  # 架构设计文档
```

Maven 依赖方向：`backend → common → dependencies`

## 核心功能

- **认证体系**：微信小程序静默登录 + Web 管理端密码登录（AES-256 加密传输）
- **错题管理**：错题 CRUD、多级标签体系（学科/章节/知识点）、图片上传
- **复习调度**：艾宾浩斯记忆曲线自动生成复习计划、答题反馈、掌握度追踪
- **管理后台**：用户管理、标签管理、系统配置、操作日志、数据统计

## 快速开始

> 详细的保姆级安装步骤请查看 [部署指南](docs/部署指南.md)。

### 前置条件

- Git
- Docker Desktop（用于运行 MySQL 和 Redis）
- JDK 17
- Maven 3.8+
- Node.js 20+
- pnpm 10+

### 启动步骤

```bash
# 1. 克隆项目
git clone https://github.com/JianZJ-75/mistake-hub.git
cd mistake-hub

# 2. 启动 MySQL 和 Redis
docker compose up -d

# 3. 初始化数据库
docker exec -i mysql mysql -uroot -p'mysql-1q2w3e4R' < resources/db/mistake-hub.sql
docker exec -i mysql mysql -uroot -p'mysql-1q2w3e4R' mistake_hub < backend/src/main/resources/sql/V6__system_config_and_operation_log.sql
# （可选）导入测试数据
docker exec -i mysql mysql -uroot -p'mysql-1q2w3e4R' mistake_hub < resources/db/mock-data.sql

# 4. 启动后端
cd dependencies && mvn install -Dmaven.test.skip=true -q && cd ..
cd backend && mvn spring-boot:run -Dmaven.test.skip=true

# 5. 启动 Web 管理端（新终端窗口）
cd frontend/mistake-hub-web && pnpm install && pnpm dev

# 6.（可选）启动微信小程序（新终端窗口）
cd frontend/mistake-hub-wechat && npm install && npm run dev:weapp
# 用微信开发者工具打开 dist/ 目录
```

> **Windows 用户注意**：步骤 3 的 `<` 重定向在 PowerShell 中不可用，请使用：
> ```powershell
> Get-Content resources\db\mistake-hub.sql | docker exec -i mysql mysql -uroot -p'mysql-1q2w3e4R'
> ```

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin-123 | 管理员 |

首次启动后端时自动创建，无需手动插入。

## API 文档

后端启动后访问 Swagger UI：

```
http://localhost:18080/actuator/swagger-ui/index.html
```

## 端口汇总

| 服务 | 端口 | 地址 |
|------|------|------|
| 后端 API | 8080 | http://localhost:8080/mistake-hub/backend |
| Swagger UI | 18080 | http://localhost:18080/actuator/swagger-ui/index.html |
| Web 管理端 | 5173 | http://localhost:5173 |
| MySQL | 3306 | 127.0.0.1:3306 |
| Redis | 6379 | 127.0.0.1:6379 |

## 项目文档

- [架构设计](ARCHITECTURE.md) - 技术栈、模块结构、核心业务流程
- [部署指南](docs/部署指南.md) - 保姆级环境搭建与启动教程
- [实施规划](md/实施规划.md) - 迭代计划与任务清单
- [编码规范](CLAUDE.md) - 后端 Java + 前端 TypeScript 编码标准
