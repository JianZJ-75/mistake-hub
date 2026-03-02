# 后端迭代2实施规划 - 需求文档

基于迭代1已完成的认证体系和用户管理，建立错题核心数据模型，实现错题的完整 CRUD、标签管理和图片上传功能。

## Core Features

### 2.1~2.3 数据库表设计

- 建立 `mistake`（错题表）、`tag`（标签表）、`mistake_tag`（错题标签关联表）三张核心数据表
- mistake 表包含题干、答案、错误原因、图片、学科、复习阶段、掌握度等字段
- tag 表支持三级层级结构：学科(SUBJECT) → 章节(CHAPTER) → 知识点(KNOWLEDGE)
- mistake_tag 实现错题与标签的多对多关联

### 2.4 Entity + Mapper

- 创建 Mistake、Tag、MistakeTag 三个实体类及对应 Mapper
- 遵循现有 Entity 规范（Serializable、@TableId、Lombok 注解等）

### 2.5 错题录入接口

- POST `/v1/mistake/add`
- 接收题干、正确答案、错误原因、学科、标签列表
- 新增错题时自动初始化 review_stage=0、mastery_level=0、next_review_time=当前时间
- 同时写入 mistake_tag 关联表

### 2.6 图片上传接口

- POST `/v1/upload/image`
- 接收图片文件（MultipartFile），校验类型和大小
- 存储到本地文件系统，返回可访问的 URL 路径
- 配置 Spring Boot 静态资源映射，使上传文件可通过 HTTP 访问

### 2.7 标签管理接口

- POST `/v1/tag/tree` 返回完整的三级标签树结构
- POST `/v1/tag/add` 新增标签，支持指定父标签

### 2.8 错题列表查询

- POST `/v1/mistake/list`
- 支持按学科、标签、掌握状态、关键字筛选
- 分页返回，每条记录包含关联的标签信息

### 2.9 错题详情/编辑/删除

- POST `/v1/mistake/detail` 返回错题详细信息（含标签）
- POST `/v1/mistake/update` 更新错题信息（含标签重新关联）
- POST `/v1/mistake/delete` 软删除错题（status=0）

### 2.10 错题状态初始化

- 新增错题时自动设置初始复习状态，使其自动进入复习调度池

## User Stories

- 作为学生，我可以录入一道新的错题，包含题干文字、正确答案、错误原因和图片
- 作为学生，我可以为错题选择学科和知识点标签进行分类
- 作为学生，我可以按学科、知识点筛选错题列表
- 作为学生，我可以查看、编辑和删除自己的错题
- 作为学生，我上传的错题图片可以正常显示
- 作为管理员，我可以管理标签体系（新增标签）

## Acceptance Criteria

- [ ] mistake、tag、mistake_tag 三张表成功创建
- [ ] POST `/v1/mistake/add` 成功创建错题，review_stage=0、mastery_level=0、next_review_time=当前时间
- [ ] POST `/v1/upload/image` 上传图片成功，返回可访问 URL
- [ ] POST `/v1/tag/tree` 返回正确的三级树结构
- [ ] POST `/v1/tag/add` 成功新增标签，同父下不重名
- [ ] POST `/v1/mistake/list` 按学科筛选返回正确结果
- [ ] POST `/v1/mistake/list` 按标签筛选返回正确结果
- [ ] POST `/v1/mistake/detail` 返回完整错题信息（含标签列表）
- [ ] POST `/v1/mistake/update` 更新错题及标签关联成功
- [ ] POST `/v1/mistake/delete` 软删除后列表不再显示
- [ ] 错题只能被所属用户查看和操作（权限隔离）

## Non-functional Requirements

- **安全**：错题 CRUD 操作必须校验 account_id 归属，防止越权操作
- **性能**：列表查询使用分页 + 索引，标签加载使用批量查询避免 N+1
- **数据完整性**：软删除保留数据，标签关联在更新时先删后插保证一致性
- **文件上传**：限制图片类型（jpg/png/gif）和大小（≤5MB），使用 UUID 命名防止冲突

## 依赖的后端接口（迭代1已完成）

| 接口 | URL | 说明 |
|------|-----|------|
| 认证拦截器 | — | Bearer token 校验，未认证返回 403 |
| ThreadStorageUtil | — | 获取当前用户 accountId |
| @PreAuthorize | — | 角色级别权限校验 |
