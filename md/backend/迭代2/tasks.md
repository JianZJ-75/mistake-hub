# 后端迭代2 - 任务清单

## Implementation Tasks

- [ ] 1. **2.1~2.3 数据库表创建**
    - [ ] 1.1. 新增 mistake、tag、mistake_tag 建表语句
        - *Goal*: 在 `resources/db/mistake-hub.sql` 中追加三张表的 DDL
        - *Details*: 按设计文档中的 DDL 添加 mistake（含 idx_account_status、idx_next_review 索引）、tag（含 uk_name_parent 唯一索引）、mistake_tag（含 uk_mistake_tag 唯一索引）三张表。在 MySQL 中执行 DDL 确认建表成功
        - *Requirements*: 2.1, 2.2, 2.3

- [ ] 2. **2.4 Entity + Mapper**
    - [ ] 2.1. 创建 Mistake 实体类
        - *Goal*: 在 `entity/` 下创建 `Mistake.java`
        - *Details*: 按设计文档定义所有字段（id, accountId, title, correctAnswer, errorReason, imageUrl, subject, reviewStage, masteryLevel, lastReviewTime, nextReviewTime, status, createdTime, updatedTime），注解 `@Getter @Setter @ToString @TableName("mistake") @Builder @NoArgsConstructor @AllArgsConstructor`，实现 `Serializable`，每个字段中文注释
        - *Requirements*: 2.4
    - [ ] 2.2. 创建 Tag 实体类
        - *Goal*: 在 `entity/` 下创建 `Tag.java`
        - *Details*: 字段：id, name, type, parentId, createdTime。注解同 Mistake
        - *Requirements*: 2.4
    - [ ] 2.3. 创建 MistakeTag 实体类
        - *Goal*: 在 `entity/` 下创建 `MistakeTag.java`
        - *Details*: 字段：id, mistakeId, tagId。注解同 Mistake
        - *Requirements*: 2.4
    - [ ] 2.4. 创建三个 Mapper 接口
        - *Goal*: 在 `mapper/` 下创建 `MistakeMapper.java`、`TagMapper.java`、`MistakeTagMapper.java`
        - *Details*: 均继承 `BaseMapper<XxxEntity>`，不加 `@Mapper` 注解（启动类 @MapperScan 统一扫描）
        - *Requirements*: 2.4

- [ ] 3. **创建 TagType 枚举**
    - [ ] 3.1. 创建 TagType 枚举类
        - *Goal*: 在 `enums/` 下创建 `TagType.java`
        - *Details*: 三个值 SUBJECT/CHAPTER/KNOWLEDGE，标准四字段 code/displayNameCn/displayNameUs/level，标准静态方法集 fromCode/isValid/isInvalid。参照 Role.java 的结构
        - *Requirements*: 2.7

- [ ] 4. **创建 DTO 对象**
    - [ ] 4.1. 创建请求对象
        - *Goal*: 在 `dto/req/` 下创建 MistakeAddReq、MistakeUpdateReq、MistakeListReq、MistakeDetailReq、MistakeDeleteReq、TagAddReq
        - *Details*: 按设计文档中的字段定义和校验注解创建。所有 Req 使用 `@Data`，校验 message 为中文
        - *Requirements*: 2.5, 2.7, 2.8, 2.9
    - [ ] 4.2. 创建响应对象
        - *Goal*: 在 `dto/resp/` 下创建 MistakeDetailResp、TagResp、TagTreeResp
        - *Details*: 按设计文档定义字段。所有 Resp 使用 `@Data @Builder @NoArgsConstructor @AllArgsConstructor`。MistakeDetailResp 中包含 `List<TagResp> tags` 字段。TagTreeResp 中包含 `List<TagTreeResp> children` 递归字段
        - *Requirements*: 2.5, 2.7, 2.8, 2.9

- [ ] 5. **图片上传功能**
    - [ ] 5.1. 创建 UploadProperties 配置类
        - *Goal*: 在 `config/` 下创建 `UploadProperties.java`
        - *Details*: `@Data @Component @ConfigurationProperties(prefix = "upload")`，字段 `private String path`。在 `application.yml` 中配置 `upload.path: ${user.home}/mistake-hub-uploads`
        - *Requirements*: 2.6
    - [ ] 5.2. 配置 multipart 和静态资源
        - *Goal*: 在 Spring Boot 配置中启用文件上传并映射静态资源
        - *Details*: 在 `application.yml` 中添加 `spring.servlet.multipart.max-file-size: 5MB` 和 `max-request-size: 10MB`。在 `CustomizedWebMvcConfig` 中重写 `addResourceHandlers`，将 `/uploads/**` 映射到 `file:${upload.path}/`。在 `auth-check.exclude-path-list` 中添加 `/uploads/**` 使图片资源无需认证
        - *Requirements*: 2.6
    - [ ] 5.3. 创建 UploadService
        - *Goal*: 在 `service/` 下创建 `UploadService.java`
        - *Details*: 注入 UploadProperties。`uploadImage(MultipartFile)` 方法：校验 MIME 类型（jpg/png/gif）→ 校验大小（≤5MB）→ UUID 文件名 → 创建 images 子目录 → 写入磁盘 → 返回 `/uploads/images/{uuid}.{ext}`。异常使用 `oops()` 抛出
        - *Requirements*: 2.6
    - [ ] 5.4. 创建 UploadController
        - *Goal*: 在 `controller/` 下创建 `UploadController.java`
        - *Details*: `@Tag(name = "文件上传") @RestController @RequestMapping("/v1/upload") @Validated @Slf4j`。`uploadImage` 方法使用 `@RequestParam("file") MultipartFile file`，权限 `@PreAuthorize(requiredRole = Role.STUDENT)`
        - *Requirements*: 2.6

- [ ] 6. **标签管理功能**
    - [ ] 6.1. 创建 TagService
        - *Goal*: 在 `service/` 下创建 `TagService.java`
        - *Details*: 继承 `ServiceImpl<TagMapper, Tag>`。实现 `tree()` 方法（查全量标签 → 按 parentId 分组 → 递归构建 TagTreeResp 树）和 `add(TagAddReq)` 方法（校验 type → 校验层级关系 → 校验同父不重名 → save）。工具方法：`buildTree`、`validateParent`、`getByNameAndParent`
        - *Requirements*: 2.7
    - [ ] 6.2. 创建 TagController
        - *Goal*: 在 `controller/` 下创建 `TagController.java`
        - *Details*: `@Tag(name = "标签") @RestController @RequestMapping("/v1/tag") @Validated @Slf4j`。两个方法：`tree()` 和 `add(@RequestBody @Valid TagAddReq)`，权限均为 `Role.STUDENT`
        - *Requirements*: 2.7

- [ ] 7. **错题标签关联服务**
    - [ ] 7.1. 创建 MistakeTagService
        - *Goal*: 在 `service/` 下创建 `MistakeTagService.java`
        - *Details*: 继承 `ServiceImpl<MistakeTagMapper, MistakeTag>`。实现四个方法：`batchSave(mistakeId, tagIds)`（批量插入关联）、`removeByMistakeId(mistakeId)`（删除错题所有关联）、`getTagsByMistakeIds(mistakeIds)`（批量查询返回 `Map<Long, List<Tag>>`）、`getMistakeIdsByTagIds(tagIds)`（按标签查错题ID列表）。注入 TagService 获取 tag 详情
        - *Requirements*: 2.5, 2.8, 2.9

- [ ] 8. **错题 CRUD 功能**
    - [ ] 8.1. 创建 MistakeService
        - *Goal*: 在 `service/` 下创建 `MistakeService.java`
        - *Details*: 继承 `ServiceImpl<MistakeMapper, Mistake>`。注入 ThreadStorageUtil、MistakeTagService。实现五个业务方法：`add`（构建 Mistake + save + 批量关联标签）、`list`（标签预筛选 + 组合条件分页 + 批量加载标签）、`detail`（校验归属 + 加载标签）、`update`（校验归属 + 更新字段 + 标签全量覆盖）、`delete`（校验归属 + status=0）。工具方法：`toDetailResp`、`toTagResp`、`getAndCheckOwnership`
        - *Requirements*: 2.5, 2.8, 2.9, 2.10
    - [ ] 8.2. 创建 MistakeController
        - *Goal*: 在 `controller/` 下创建 `MistakeController.java`
        - *Details*: `@Tag(name = "错题") @RestController @RequestMapping("/v1/mistake") @Validated @Slf4j`。五个方法：add、list、detail、update、delete，全部使用 `@PostMapping`，权限为 `Role.STUDENT`。方法签名使用 `@RequestBody @Valid XxxReq`
        - *Requirements*: 2.5, 2.8, 2.9

- [ ] 9. **编译验证**
    - [ ] 9.1. 编译并启动后端
        - *Goal*: 确保所有新增代码编译通过，应用正常启动
        - *Details*: 执行 `cd dependencies && mvn install -Dmaven.test.skip=true -q && cd ../backend && mvn spring-boot:run -Dmaven.test.skip=true`，验证启动无报错。检查 Swagger 文档中是否出现新增的"错题"、"标签"、"文件上传"三个 Tag
        - *Requirements*: 全部

## Task Dependencies

- 任务 1.1（建表）必须最先完成，Entity 依赖表结构
- 任务 2（Entity + Mapper）依赖任务 1
- 任务 3（TagType 枚举）可与任务 2 并行
- 任务 4（DTO）可与任务 2 并行
- 任务 5（上传功能）依赖任务 4.2（UploadController 不依赖 DTO，但同属基础设施）
- 任务 6（标签管理）依赖任务 2 + 3 + 4
- 任务 7（MistakeTagService）依赖任务 2 + 6
- 任务 8（错题 CRUD）依赖任务 4 + 7
- 任务 9（编译验证）依赖所有任务

```
1.1 (建表) → 2 (Entity/Mapper)
                 ↓
3 (枚举) ──→ 6 (TagService) ──→ 7 (MistakeTagService) ──→ 8 (MistakeService/Controller)
                 ↓                                              ↓
4 (DTO) ────→ 5 (Upload) ──────────────────────────────────→ 9 (编译验证)
```
