# 后端迭代2 - 设计文档

## Overview

迭代2 后端的核心目标是建立错题数据模型，实现完整的错题 CRUD、标签分类管理和图片上传功能。本迭代新增 3 张数据库表、3 个 Entity、3 个 Mapper、4 个 Service、3 个 Controller、1 个枚举和多个 DTO 对象。

## 数据库设计

### 新增表 DDL

```sql
-- 错题表
CREATE TABLE `mistake` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account_id`       BIGINT       NOT NULL COMMENT '所属用户',
    `title`            TEXT         NOT NULL COMMENT '题干内容',
    `correct_answer`   TEXT                  COMMENT '正确答案',
    `error_reason`     TEXT                  COMMENT '错误原因说明',
    `subject`          VARCHAR(64)           COMMENT '学科',
    `review_stage`     INT          NOT NULL DEFAULT 0  COMMENT '复习阶段 0-6',
    `mastery_level`    INT          NOT NULL DEFAULT 0  COMMENT '掌握度 0-100',
    `last_review_time` DATETIME              COMMENT '最近复习时间',
    `next_review_time` DATETIME              COMMENT '下次复习时间',
    `status`           TINYINT      NOT NULL DEFAULT 1  COMMENT '1-有效 0-删除',
    `created_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`     DATETIME              NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_account_status` (`account_id`, `status`),
    INDEX `idx_next_review` (`account_id`, `next_review_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '错题表';

-- 标签表
CREATE TABLE `tag` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`         VARCHAR(128) NOT NULL COMMENT '标签名称',
    `type`         VARCHAR(32)  NOT NULL COMMENT '类型: SUBJECT/CHAPTER/KNOWLEDGE',
    `parent_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '父标签ID，0为顶级',
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name_parent` (`name`, `parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '标签表';

-- 错题标签关联表
CREATE TABLE `mistake_tag` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mistake_id` BIGINT NOT NULL COMMENT '错题ID',
    `tag_id`     BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_mistake_tag` (`mistake_id`, `tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '错题标签关联表';

-- 错题图片表
CREATE TABLE `mistake_image` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `mistake_id`  BIGINT       NOT NULL COMMENT '所属错题ID',
    `image_url`   VARCHAR(512) NOT NULL COMMENT '图片相对路径',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号（从0开始）',
    PRIMARY KEY (`id`),
    INDEX `idx_mistake_id` (`mistake_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '错题图片表';
```

### 索引说明

| 表 | 索引 | 用途 |
|---|------|------|
| mistake | idx_account_status | 按用户 + 状态查询错题列表 |
| mistake | idx_next_review | 复习调度查询（迭代 3 使用） |
| tag | uk_name_parent | 防止同一父标签下出现重名 |
| mistake_tag | uk_mistake_tag | 防止重复关联 |
| mistake_image | idx_mistake_id | 按错题 ID 查询图片列表 |

## 新增文件清单

```
backend/src/main/java/com/jianzj/mistake/hub/backend/
├── entity/
│   ├── Mistake.java              ← 新增
│   ├── Tag.java                  ← 新增
│   ├── MistakeTag.java           ← 新增
│   └── MistakeImage.java         ← 新增
├── mapper/
│   ├── MistakeMapper.java        ← 新增
│   ├── TagMapper.java            ← 新增
│   ├── MistakeTagMapper.java     ← 新增
│   └── MistakeImageMapper.java   ← 新增
├── service/
│   ├── MistakeService.java       ← 新增
│   ├── TagService.java           ← 新增
│   ├── MistakeTagService.java    ← 新增
│   ├── MistakeImageService.java  ← 新增
│   └── UploadService.java        ← 新增
├── controller/
│   ├── MistakeController.java    ← 新增
│   ├── TagController.java        ← 新增
│   └── UploadController.java     ← 新增
├── dto/
│   ├── req/
│   │   ├── MistakeAddReq.java    ← 新增
│   │   ├── MistakeUpdateReq.java ← 新增
│   │   ├── MistakeListReq.java   ← 新增
│   │   ├── MistakeDetailReq.java ← 新增
│   │   ├── MistakeDeleteReq.java ← 新增
│   │   └── TagAddReq.java        ← 新增
│   └── resp/
│       ├── MistakeDetailResp.java ← 新增
│       ├── TagResp.java           ← 新增
│       └── TagTreeResp.java       ← 新增
├── enums/
│   └── TagType.java              ← 新增
└── config/
    └── UploadProperties.java     ← 新增
```

修改文件：
- `CustomizedWebMvcConfig.java`：添加静态资源映射（`/uploads/**`）
- `application.yml`：添加上传配置、白名单路径
- `resources/db/mistake-hub.sql`：添加新表 DDL

## Entity 设计

### Mistake 实体

```java
@Getter @Setter @ToString
@TableName("mistake")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class Mistake implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long accountId;

    /** 题干内容 */
    private String title;

    /** 正确答案 */
    private String correctAnswer;

    /** 错误原因说明 */
    private String errorReason;

    /** 学科 */
    private String subject;

    /** 复习阶段 0-6 */
    private Integer reviewStage;

    /** 掌握度 0-100 */
    private Integer masteryLevel;

    /** 最近复习时间 */
    private LocalDateTime lastReviewTime;

    /** 下次复习时间 */
    private LocalDateTime nextReviewTime;

    /** 状态：1-有效 0-删除 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
```

### Tag 实体

```java
@Getter @Setter @ToString
@TableName("tag")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String name;

    /** 类型: SUBJECT/CHAPTER/KNOWLEDGE */
    private String type;

    /** 父标签ID，0为顶级 */
    private Long parentId;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
```

### MistakeTag 实体

```java
@Getter @Setter @ToString
@TableName("mistake_tag")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MistakeTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 错题ID */
    private Long mistakeId;

    /** 标签ID */
    private Long tagId;
}
```

### MistakeImage 实体

```java
@Getter @Setter @ToString
@TableName("mistake_image")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MistakeImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属错题ID */
    private Long mistakeId;

    /** 图片相对路径 */
    private String imageUrl;

    /** 排序序号（从0开始） */
    private Integer sortOrder;
}
```

## 枚举设计

### TagType 枚举

```java
@Getter
@AllArgsConstructor
public enum TagType {

    SUBJECT("SUBJECT", "学科", "Subject", 1),
    CHAPTER("CHAPTER", "章节", "Chapter", 2),
    KNOWLEDGE("KNOWLEDGE", "知识点", "Knowledge", 3)
    ;

    private final String code;
    private final String displayNameCn;
    private final String displayNameUs;
    private final int level;

    public static TagType fromCode(String code) { ... }
    public static boolean isValid(String code) { ... }
    public static boolean isInvalid(String code) { ... }
}
```

**层级校验规则：**

| 新增标签类型 | parentId 要求 | 父标签类型要求 |
|-------------|-------------|--------------|
| SUBJECT | 必须为 0（顶级） | 无 |
| CHAPTER | 必须 > 0 | 必须是 SUBJECT |
| KNOWLEDGE | 必须 > 0 | 必须是 CHAPTER |

## DTO 设计

### 请求对象

**MistakeAddReq**

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| title | String | @NotBlank(message = "题干内容不能为空") | 题干内容 |
| correctAnswer | String | 可选 | 正确答案 |
| errorReason | String | 可选 | 错误原因 |
| imageUrls | List\<String\> | 可选 | 图片URL列表（先逐张调上传接口获取URL，再提交） |
| subject | String | 可选 | 学科 |
| tagIds | List\<Long\> | 可选 | 关联标签ID列表 |

**MistakeUpdateReq**

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| id | Long | @NotNull(message = "错题 ID 不能为空") | 错题ID |
| title | String | 可选 | 题干内容 |
| correctAnswer | String | 可选 | 正确答案 |
| errorReason | String | 可选 | 错误原因 |
| imageUrls | List\<String\> | 可选 | 图片URL列表（全量覆盖：传入时先删旧图片再保存新图片） |
| subject | String | 可选 | 学科 |
| tagIds | List\<Long\> | 可选 | 关联标签ID列表（全量覆盖：传入时先删旧关联再建新关联） |

**MistakeListReq**

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| subject | String | 可选 | 学科筛选 |
| tagIds | List\<Long\> | 可选 | 标签ID筛选（OR 逻辑，匹配任一标签即命中） |
| masteryMin | Integer | 可选 | 掌握度最小值 |
| masteryMax | Integer | 可选 | 掌握度最大值 |
| keyword | String | 可选 | 题干关键字模糊搜索 |
| pageNum | Long | @NotNull @Min(1) | 页码 |
| pageSize | Long | @NotNull @Min(1) | 每页数量 |

**MistakeDetailReq / MistakeDeleteReq**

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| id | Long | @NotNull(message = "错题 ID 不能为空") | 错题ID |

**TagAddReq**

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| name | String | @NotBlank(message = "标签名称不能为空") | 标签名称 |
| type | String | @NotBlank(message = "标签类型不能为空") | 标签类型（SUBJECT/CHAPTER/KNOWLEDGE） |
| parentId | Long | 可选，默认为 0 | 父标签ID，顶级标签传 0 或不传 |

### 响应对象

**MistakeDetailResp**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 错题ID |
| title | String | 题干内容 |
| correctAnswer | String | 正确答案 |
| errorReason | String | 错误原因 |
| imageUrls | List\<String\> | 图片URL列表（按 sort_order 升序） |
| subject | String | 学科 |
| reviewStage | Integer | 复习阶段 |
| masteryLevel | Integer | 掌握度 |
| lastReviewTime | LocalDateTime | 最近复习时间 |
| nextReviewTime | LocalDateTime | 下次复习时间 |
| tags | List\<TagResp\> | 关联标签列表 |
| createdTime | LocalDateTime | 创建时间 |
| updatedTime | LocalDateTime | 更新时间 |

**TagResp**（内嵌在 MistakeDetailResp.tags 中）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 标签ID |
| name | String | 标签名称 |
| type | String | 标签类型 |

**TagTreeResp**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 标签ID |
| name | String | 标签名称 |
| type | String | 标签类型 |
| children | List\<TagTreeResp\> | 子标签列表 |

标签树示例：
```json
[
  {
    "id": 1, "name": "数学", "type": "SUBJECT",
    "children": [
      {
        "id": 2, "name": "函数", "type": "CHAPTER",
        "children": [
          { "id": 3, "name": "一次函数", "type": "KNOWLEDGE", "children": [] },
          { "id": 4, "name": "二次函数", "type": "KNOWLEDGE", "children": [] }
        ]
      }
    ]
  },
  {
    "id": 5, "name": "英语", "type": "SUBJECT",
    "children": [ ... ]
  }
]
```

## API 设计

### MistakeController

```
@Tag(name = "错题")
@RestController
@RequestMapping("/v1/mistake")
@Validated
@Slf4j
```

| 方法 | URL | 权限 | 入参 | 出参 | 说明 |
|------|-----|------|------|------|------|
| add | /add | STUDENT | @RequestBody @Valid MistakeAddReq | void | 录入错题 |
| list | /list | STUDENT | @RequestBody @Valid MistakeListReq | Page\<MistakeDetailResp\> | 分页查询（仅当前用户） |
| detail | /detail | STUDENT | @RequestBody @Valid MistakeDetailReq | MistakeDetailResp | 错题详情 |
| update | /update | STUDENT | @RequestBody @Valid MistakeUpdateReq | void | 编辑错题 |
| delete | /delete | STUDENT | @RequestBody @Valid MistakeDeleteReq | void | 软删除错题 |

所有方法注解顺序：`@Operation` → `@PostMapping` → `@PreAuthorize`

### TagController

```
@Tag(name = "标签")
@RestController
@RequestMapping("/v1/tag")
@Validated
@Slf4j
```

| 方法 | URL | 权限 | 入参 | 出参 | 说明 |
|------|-----|------|------|------|------|
| tree | /tree | STUDENT | 无 | List\<TagTreeResp\> | 标签树 |
| add | /add | STUDENT | @RequestBody @Valid TagAddReq | void | 新增标签 |

### UploadController

```
@Tag(name = "文件上传")
@RestController
@RequestMapping("/v1/upload")
@Validated
@Slf4j
```

| 方法 | URL | 权限 | 入参 | 出参 | 说明 |
|------|-----|------|------|------|------|
| uploadImage | /image | STUDENT | @RequestParam("file") MultipartFile | String | 返回图片相对路径 |

> 上传接口使用 `@RequestParam` 接收 `MultipartFile`，而非 `@RequestBody`。前端通过 `multipart/form-data` 提交。

## Service 层设计

### MistakeService

继承 `ServiceImpl<MistakeMapper, Mistake>`。

**业务方法：**

```java
/** 录入错题 */
public void add(MistakeAddReq req) {

    Long accountId = threadStorageUtil.getCurAccountId();

    Mistake mistake = Mistake.builder()
            .accountId(accountId)
            .title(req.getTitle())
            .correctAnswer(req.getCorrectAnswer())
            .errorReason(req.getErrorReason())
            .subject(req.getSubject())
            .reviewStage(0)
            .masteryLevel(0)
            .nextReviewTime(LocalDateTime.now())
            .status(1)
            .build();

    boolean success = save(mistake);
    if (!success) {
        oops("录入错题失败", "Failed to add mistake.");
    }

    // 批量保存图片
    if (CollectionUtils.isNotEmpty(req.getImageUrls())) {
        mistakeImageService.batchSave(mistake.getId(), req.getImageUrls());
    }

    // 批量插入标签关联
    if (CollectionUtils.isNotEmpty(req.getTagIds())) {
        mistakeTagService.batchSave(mistake.getId(), req.getTagIds());
    }
}

/** 分页查询错题列表 */
public Page<MistakeDetailResp> list(MistakeListReq req) {

    Long accountId = threadStorageUtil.getCurAccountId();

    // 1. 标签筛选：先查 mistake_tag 获取候选 mistakeId
    List<Long> filteredMistakeIds = null;
    if (CollectionUtils.isNotEmpty(req.getTagIds())) {
        filteredMistakeIds = mistakeTagService.getMistakeIdsByTagIds(req.getTagIds());
        if (CollectionUtils.isEmpty(filteredMistakeIds)) {
            return new Page<>(req.getPageNum(), req.getPageSize(), 0);
        }
    }

    // 2. 组合条件分页查询
    Page<Mistake> page = lambdaQuery()
            .eq(Mistake::getAccountId, accountId)
            .eq(Mistake::getStatus, 1)
            .eq(StringUtils.isNotBlank(req.getSubject()), Mistake::getSubject, req.getSubject())
            .like(StringUtils.isNotBlank(req.getKeyword()), Mistake::getTitle, req.getKeyword())
            .ge(req.getMasteryMin() != null, Mistake::getMasteryLevel, req.getMasteryMin())
            .le(req.getMasteryMax() != null, Mistake::getMasteryLevel, req.getMasteryMax())
            .in(filteredMistakeIds != null, Mistake::getId, filteredMistakeIds)
            .orderByDesc(Mistake::getCreatedTime)
            .page(new Page<>(req.getPageNum(), req.getPageSize()));

    // 3. 批量加载标签（避免 N+1）
    List<Long> mistakeIds = page.getRecords().stream().map(Mistake::getId).toList();
    Map<Long, List<Tag>> tagMap = CollectionUtils.isEmpty(mistakeIds)
            ? Map.of()
            : mistakeTagService.getTagsByMistakeIds(mistakeIds);

    // 4. 批量加载图片（避免 N+1）
    Map<Long, List<String>> imageMap = CollectionUtils.isEmpty(mistakeIds)
            ? Map.of()
            : mistakeImageService.getUrlsByMistakeIds(mistakeIds);

    // 5. 转换响应
    List<MistakeDetailResp> respList = page.getRecords().stream()
            .map(m -> toDetailResp(m, tagMap.getOrDefault(m.getId(), List.of()), imageMap.getOrDefault(m.getId(), List.of())))
            .toList();

    Page<MistakeDetailResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    respPage.setRecords(respList);
    return respPage;
}

/** 错题详情 */
public MistakeDetailResp detail(MistakeDetailReq req) {

    Mistake mistake = getAndCheckOwnership(req.getId());
    List<Tag> tags = mistakeTagService.getTagsByMistakeId(mistake.getId());
    List<String> imageUrls = mistakeImageService.getUrlsByMistakeId(mistake.getId());
    return toDetailResp(mistake, tags, imageUrls);
}

/** 编辑错题 */
public void update(MistakeUpdateReq req) {

    Mistake mistake = getAndCheckOwnership(req.getId());

    // 更新非空字段
    if (StringUtils.isNotBlank(req.getTitle())) mistake.setTitle(req.getTitle());
    if (req.getCorrectAnswer() != null) mistake.setCorrectAnswer(req.getCorrectAnswer());
    if (req.getErrorReason() != null) mistake.setErrorReason(req.getErrorReason());
    if (req.getSubject() != null) mistake.setSubject(req.getSubject());

    boolean success = updateById(mistake);
    if (!success) {
        oops("更新错题失败", "Failed to update mistake.");
    }

    // 图片全量覆盖
    if (req.getImageUrls() != null) {
        mistakeImageService.removeByMistakeId(mistake.getId());
        if (CollectionUtils.isNotEmpty(req.getImageUrls())) {
            mistakeImageService.batchSave(mistake.getId(), req.getImageUrls());
        }
    }

    // 标签关联全量覆盖
    if (req.getTagIds() != null) {
        mistakeTagService.removeByMistakeId(mistake.getId());
        if (CollectionUtils.isNotEmpty(req.getTagIds())) {
            mistakeTagService.batchSave(mistake.getId(), req.getTagIds());
        }
    }
}

/** 软删除错题 */
public void delete(MistakeDeleteReq req) {

    Mistake mistake = getAndCheckOwnership(req.getId());
    mistake.setStatus(0);

    boolean success = updateById(mistake);
    if (!success) {
        oops("删除错题失败", "Failed to delete mistake.");
    }
}
```

**工具方法：**

```java
/** 转换为详情响应（含标签 + 图片） */
private MistakeDetailResp toDetailResp(Mistake mistake, List<Tag> tags, List<String> imageUrls) {

    MistakeDetailResp resp = new MistakeDetailResp();
    BeanUtils.copyProperties(mistake, resp);
    resp.setTags(tags.stream().map(this::toTagResp).toList());
    resp.setImageUrls(imageUrls);
    return resp;
}

/** Tag 转 TagResp */
private TagResp toTagResp(Tag tag) {

    TagResp resp = new TagResp();
    BeanUtils.copyProperties(tag, resp);
    return resp;
}

/** 校验错题存在性 + 归属当前用户 */
private Mistake getAndCheckOwnership(Long id) {

    Mistake mistake = getById(id);
    if (mistake == null || mistake.getStatus() == 0) {
        oops("错题不存在", "Mistake not found.");
    }
    Long accountId = threadStorageUtil.getCurAccountId();
    if (!mistake.getAccountId().equals(accountId)) {
        oops("无权操作该错题", "No permission to access this mistake.");
    }
    return mistake;
}
```

### TagService

继承 `ServiceImpl<TagMapper, Tag>`。

```java
/** 获取标签树 */
public List<TagTreeResp> tree() {

    List<Tag> allTags = list();
    // 按 parentId 分组
    Map<Long, List<Tag>> parentMap = allTags.stream()
            .collect(Collectors.groupingBy(Tag::getParentId));
    // 从顶级标签（parentId=0）开始递归构建
    return buildTree(parentMap, 0L);
}

/** 新增标签 */
public void add(TagAddReq req) {

    // 1. 校验 type 合法性
    if (TagType.isInvalid(req.getType())) {
        oops("无效的标签类型", "Invalid tag type.");
    }
    TagType tagType = TagType.fromCode(req.getType());

    // 2. 校验层级关系
    Long parentId = req.getParentId() == null ? 0L : req.getParentId();
    validateParent(tagType, parentId);

    // 3. 校验同父下不重名
    Tag existing = getByNameAndParent(req.getName(), parentId);
    if (existing != null) {
        oops("同级标签下已存在相同名称", "Duplicate tag name under the same parent.");
    }

    // 4. 保存
    Tag tag = Tag.builder()
            .name(req.getName())
            .type(tagType.getCode())
            .parentId(parentId)
            .build();

    boolean success = save(tag);
    if (!success) {
        oops("新增标签失败", "Failed to add tag.");
    }
}

// ===== 工具方法 =====

/** 递归构建标签树 */
private List<TagTreeResp> buildTree(Map<Long, List<Tag>> parentMap, Long parentId) {

    List<Tag> children = parentMap.getOrDefault(parentId, List.of());
    return children.stream().map(tag -> TagTreeResp.builder()
            .id(tag.getId())
            .name(tag.getName())
            .type(tag.getType())
            .children(buildTree(parentMap, tag.getId()))
            .build()
    ).toList();
}

/** 校验父标签层级合法性 */
private void validateParent(TagType tagType, Long parentId) {

    if (tagType == TagType.SUBJECT) {
        if (parentId != 0L) {
            oops("学科标签必须为顶级标签", "Subject tag must be top-level.");
        }
        return;
    }

    if (parentId == 0L) {
        oops("章节/知识点标签必须指定父标签", "Chapter/Knowledge tag requires a parent.");
    }

    Tag parent = getById(parentId);
    if (parent == null) {
        oops("父标签不存在", "Parent tag not found.");
    }

    TagType parentType = TagType.fromCode(parent.getType());
    if (tagType == TagType.CHAPTER && parentType != TagType.SUBJECT) {
        oops("章节标签的父标签必须是学科", "Chapter tag's parent must be a Subject.");
    }
    if (tagType == TagType.KNOWLEDGE && parentType != TagType.CHAPTER) {
        oops("知识点标签的父标签必须是章节", "Knowledge tag's parent must be a Chapter.");
    }
}

/** 按名称和父标签查询 */
private Tag getByNameAndParent(String name, Long parentId) {

    List<Tag> tags = lambdaQuery()
            .eq(Tag::getName, name)
            .eq(Tag::getParentId, parentId)
            .list();
    return CollectionUtils.isEmpty(tags) ? null : tags.get(0);
}
```

### MistakeTagService

继承 `ServiceImpl<MistakeTagMapper, MistakeTag>`。

```java
/** 批量保存错题标签关联 */
public void batchSave(Long mistakeId, List<Long> tagIds) {

    List<MistakeTag> relations = tagIds.stream()
            .map(tagId -> MistakeTag.builder()
                    .mistakeId(mistakeId)
                    .tagId(tagId)
                    .build())
            .toList();
    saveBatch(relations);
}

/** 删除错题的所有标签关联 */
public void removeByMistakeId(Long mistakeId) {

    lambdaUpdate().eq(MistakeTag::getMistakeId, mistakeId).remove();
}

/** 根据单个错题ID查询关联标签 */
public List<Tag> getTagsByMistakeId(Long mistakeId) {

    return getTagsByMistakeIds(List.of(mistakeId)).getOrDefault(mistakeId, List.of());
}

/** 批量查询多个错题的关联标签，返回 Map<mistakeId, List<Tag>> */
public Map<Long, List<Tag>> getTagsByMistakeIds(List<Long> mistakeIds) {

    // 1. 查 mistake_tag 关联
    List<MistakeTag> relations = lambdaQuery()
            .in(MistakeTag::getMistakeId, mistakeIds)
            .list();
    if (CollectionUtils.isEmpty(relations)) {
        return Map.of();
    }

    // 2. 批量查 tag 详情
    List<Long> tagIds = relations.stream().map(MistakeTag::getTagId).distinct().toList();
    List<Tag> tags = tagService.listByIds(tagIds);
    Map<Long, Tag> tagMap = tags.stream().collect(Collectors.toMap(Tag::getId, t -> t));

    // 3. 按 mistakeId 分组
    return relations.stream()
            .filter(r -> tagMap.containsKey(r.getTagId()))
            .collect(Collectors.groupingBy(
                    MistakeTag::getMistakeId,
                    Collectors.mapping(r -> tagMap.get(r.getTagId()), Collectors.toList())
            ));
}

/** 根据标签ID列表查询关联的错题ID列表 */
public List<Long> getMistakeIdsByTagIds(List<Long> tagIds) {

    List<MistakeTag> relations = lambdaQuery()
            .in(MistakeTag::getTagId, tagIds)
            .list();
    return relations.stream().map(MistakeTag::getMistakeId).distinct().toList();
}
```

> 注意循环依赖：MistakeTagService 需要注入 TagService（查标签详情），TagService 不依赖 MistakeTagService，不存在循环。MistakeService 注入 MistakeTagService，MistakeTagService 注入 TagService，链路单向。

### MistakeImageService

继承 `ServiceImpl<MistakeImageMapper, MistakeImage>`。

```java
/** 批量保存图片（按传入顺序设置 sortOrder） */
public void batchSave(Long mistakeId, List<String> imageUrls) {

    List<MistakeImage> images = new ArrayList<>();
    for (int i = 0; i < imageUrls.size(); i++) {
        images.add(MistakeImage.builder()
                .mistakeId(mistakeId)
                .imageUrl(imageUrls.get(i))
                .sortOrder(i)
                .build());
    }
    saveBatch(images);
}

/** 删除错题的所有图片 */
public void removeByMistakeId(Long mistakeId) {

    lambdaUpdate().eq(MistakeImage::getMistakeId, mistakeId).remove();
}

/** 查询单个错题的图片URL列表（按 sort_order 升序） */
public List<String> getUrlsByMistakeId(Long mistakeId) {

    List<MistakeImage> images = lambdaQuery()
            .eq(MistakeImage::getMistakeId, mistakeId)
            .orderByAsc(MistakeImage::getSortOrder)
            .list();
    return images.stream().map(MistakeImage::getImageUrl).toList();
}

/** 批量查询多个错题的图片，返回 Map<mistakeId, List<String>> */
public Map<Long, List<String>> getUrlsByMistakeIds(List<Long> mistakeIds) {

    List<MistakeImage> images = lambdaQuery()
            .in(MistakeImage::getMistakeId, mistakeIds)
            .orderByAsc(MistakeImage::getSortOrder)
            .list();
    if (CollectionUtils.isEmpty(images)) {
        return Map.of();
    }
    return images.stream()
            .collect(Collectors.groupingBy(
                    MistakeImage::getMistakeId,
                    Collectors.mapping(MistakeImage::getImageUrl, Collectors.toList())
            ));
}
```

### UploadService

不继承 ServiceImpl（无对应 Entity/Mapper）。

```java
@Service
@Slf4j
public class UploadService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final UploadProperties uploadProperties;

    public UploadService(UploadProperties uploadProperties) {

        this.uploadProperties = uploadProperties;
    }

    /** 上传图片，返回相对 URL 路径 */
    public String uploadImage(MultipartFile file) {

        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            oops("仅支持 jpg/png/gif 图片", "Only jpg/png/gif images are allowed.");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            oops("图片大小不能超过 5MB", "Image size must not exceed 5MB.");
        }

        // 3. 生成 UUID 文件名
        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 4. 创建目录 + 写入磁盘
        Path dirPath = Paths.get(uploadProperties.getPath(), "images");
        try {
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("图片上传失败", e);
            oops("图片上传失败", "Failed to upload image.");
        }

        // 5. 返回相对路径
        return "/uploads/images/" + fileName;
    }
}
```

## 文件上传配置

### UploadProperties

```java
@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /** 文件存储根目录 */
    private String path;
}
```

### application.yml

```yaml
upload:
  path: ${user.home}/mistake-hub-uploads

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
```

### 静态资源映射

在 `CustomizedWebMvcConfig` 中添加：

```java
private final UploadProperties uploadProperties;

// 构造函数新增参数

@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {

    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadProperties.getPath() + "/");
}
```

### 白名单

在 `application.yml` 的 `auth-check.exclude-path-list` 中添加 `/uploads/**`，使图片资源无需认证即可访问。

## Error Handling

| 场景 | 中文消息 | 英文消息 |
|------|---------|---------|
| 错题不存在 | 错题不存在 | Mistake not found. |
| 无权操作 | 无权操作该错题 | No permission to access this mistake. |
| 录入失败 | 录入错题失败 | Failed to add mistake. |
| 更新失败 | 更新错题失败 | Failed to update mistake. |
| 删除失败 | 删除错题失败 | Failed to delete mistake. |
| 文件类型不合法 | 仅支持 jpg/png/gif 图片 | Only jpg/png/gif images are allowed. |
| 文件过大 | 图片大小不能超过 5MB | Image size must not exceed 5MB. |
| 上传失败 | 图片上传失败 | Failed to upload image. |
| 标签类型无效 | 无效的标签类型 | Invalid tag type. |
| 同级重名 | 同级标签下已存在相同名称 | Duplicate tag name under the same parent. |
| 父标签不存在 | 父标签不存在 | Parent tag not found. |
| 层级不匹配 | 章节标签的父标签必须是学科 / 知识点标签的父标签必须是章节 | Chapter/Knowledge parent type mismatch. |
| 新增标签失败 | 新增标签失败 | Failed to add tag. |

## Testing Strategy

| # | 测试场景 | 输入 | 预期输出 |
|---|----------|------|----------|
| 1 | 成功录入错题（含2张图片） | title="求证勾股定理", imageUrls=["/uploads/a.jpg","/uploads/b.jpg"] | 成功，mistake_image 表插入2条记录 |
| 2 | 录入错题不传 title | title=空 | 400：题干内容不能为空 |
| 3 | 上传 jpg 图片 | 3MB jpg 文件 | 返回 /uploads/images/uuid.jpg，可通过 URL 访问 |
| 4 | 上传超大文件 | 10MB 文件 | 报错：图片大小不能超过 5MB |
| 5 | 上传非图片文件 | .txt 文件 | 报错：仅支持 jpg/png/gif |
| 6 | 查询错题列表（无筛选） | pageNum=1, pageSize=10 | 返回当前用户的所有有效错题，每条含 imageUrls 数组 |
| 7 | 按学科筛选 | subject="数学" | 只返回学科为"数学"的错题 |
| 8 | 按标签筛选 | tagIds=[1,2] | 返回包含标签1或标签2的错题 |
| 9 | 查看他人错题详情 | 其他用户的 mistakeId | 报错：无权操作该错题 |
| 10 | 软删除后查列表 | 删除一条后查询 | 被删除的不出现在列表中 |
