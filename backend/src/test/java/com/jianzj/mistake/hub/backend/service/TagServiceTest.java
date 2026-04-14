package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jianzj.mistake.hub.backend.dto.req.TagAddReq;
import com.jianzj.mistake.hub.backend.dto.req.TagCustomAddReq;
import com.jianzj.mistake.hub.backend.dto.req.TagDeleteReq;
import com.jianzj.mistake.hub.backend.dto.req.TagUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.TagResp;
import com.jianzj.mistake.hub.backend.entity.Tag;
import com.jianzj.mistake.hub.backend.mapper.MistakeTagMapper;
import com.jianzj.mistake.hub.backend.mapper.TagMapper;
import com.jianzj.mistake.hub.backend.support.MockChainHelper;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>TagService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagMapper mockTagMapper;

    @Mock
    private MistakeTagMapper mistakeTagMapper;

    @Mock
    private ThreadStorageUtil threadStorageUtil;

    private TagService tagService;

    @BeforeEach
    void setUp() {

        tagService = spy(new TagService(mistakeTagMapper, threadStorageUtil));
        ReflectionTestUtils.setField(tagService, "baseMapper", mockTagMapper);
    }

    // ===== add =====

    @Test
    void add_success() {

        stubLambdaQueryChain(List.of());
        doReturn(true).when(tagService).save(any(Tag.class));

        TagAddReq req = new TagAddReq();
        req.setName("数学");
        req.setParentId(0L);

        tagService.add(req);

        verify(tagService).save(any(Tag.class));
    }

    @Test
    void add_parentNotExist_shouldThrow() {

        doReturn(null).when(tagService).getById(999L);

        TagAddReq req = new TagAddReq();
        req.setName("函数");
        req.setParentId(999L);

        assertThatThrownBy(() -> tagService.add(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void add_duplicateName_shouldThrow() {

        Tag existing = Tag.builder().id(1L).name("数学").parentId(0L).build();
        stubLambdaQueryChain(List.of(existing));

        TagAddReq req = new TagAddReq();
        req.setName("数学");
        req.setParentId(0L);

        assertThatThrownBy(() -> tagService.add(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== modify =====

    @Test
    void modify_success() {

        Tag existing = Tag.builder().id(1L).name("旧名").parentId(0L).build();
        // 第一次 lambdaQuery 查 getTagById, 第二次查重名
        stubLambdaQueryChainSequential(List.of(existing), List.of());
        doReturn(true).when(tagService).updateById(any(Tag.class));

        TagUpdateReq req = new TagUpdateReq();
        req.setId(1L);
        req.setName("新名");
        req.setParentId(0L);

        tagService.modify(req);

        verify(tagService).updateById(any(Tag.class));
    }

    @Test
    void modify_selfAsParent_shouldThrow() {

        Tag existing = Tag.builder().id(1L).name("数学").parentId(0L).build();
        stubLambdaQueryChain(List.of(existing));

        TagUpdateReq req = new TagUpdateReq();
        req.setId(1L);
        req.setName("数学");
        req.setParentId(1L);

        assertThatThrownBy(() -> tagService.modify(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void modify_duplicateName_shouldThrow() {

        Tag existing = Tag.builder().id(1L).name("旧名").parentId(0L).build();
        Tag duplicate = Tag.builder().id(2L).name("物理").parentId(0L).build();
        stubLambdaQueryChainSequential(List.of(existing), List.of(duplicate));

        TagUpdateReq req = new TagUpdateReq();
        req.setId(1L);
        req.setName("物理");
        req.setParentId(0L);

        assertThatThrownBy(() -> tagService.modify(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== delete =====

    @Test
    void delete_success() {

        Tag existing = Tag.builder().id(1L).name("数学").parentId(0L).build();
        // getTagById → existing, 查子标签 → 空
        stubLambdaQueryChainSequential(List.of(existing), List.of());
        when(mistakeTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(true).when(tagService).removeById(1L);

        TagDeleteReq req = new TagDeleteReq();
        req.setId(1L);

        tagService.delete(req);

        verify(tagService).removeById(1L);
    }

    @Test
    void delete_hasChildren_shouldThrow() {

        Tag existing = Tag.builder().id(1L).name("数学").parentId(0L).build();
        Tag child = Tag.builder().id(2L).name("函数").parentId(1L).build();
        stubLambdaQueryChainSequential(List.of(existing), List.of(child));

        TagDeleteReq req = new TagDeleteReq();
        req.setId(1L);

        assertThatThrownBy(() -> tagService.delete(req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void delete_hasReferences_shouldThrow() {

        Tag existing = Tag.builder().id(1L).name("数学").parentId(0L).build();
        stubLambdaQueryChainSequential(List.of(existing), List.of());
        when(mistakeTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        TagDeleteReq req = new TagDeleteReq();
        req.setId(1L);

        assertThatThrownBy(() -> tagService.delete(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== tree =====

    @Test
    void tree_buildHierarchy() {

        Tag root = Tag.builder().id(1L).name("数学").parentId(0L).build();
        Tag child = Tag.builder().id(2L).name("函数").parentId(1L).build();
        Tag grandchild = Tag.builder().id(3L).name("一次函数").parentId(2L).build();

        stubLambdaQueryChain(List.of(root, child, grandchild));

        List<TagResp> tree = tagService.tree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("数学");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    // ===== addCustom =====

    @Test
    void addCustom_success() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        stubLambdaQueryChain(List.of());
        doReturn(true).when(tagService).save(any(Tag.class));

        TagCustomAddReq req = new TagCustomAddReq();
        req.setName("我的标签");

        tagService.addCustom(req);

        verify(tagService).save(any(Tag.class));
    }

    // ===== deleteCustom =====

    @Test
    void deleteCustom_notOwner_shouldThrow() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(200L);

        Tag tag = Tag.builder().id(1L).name("标签").accountId(100L).build();
        stubLambdaQueryChain(List.of(tag));

        TagDeleteReq req = new TagDeleteReq();
        req.setId(1L);

        assertThatThrownBy(() -> tagService.deleteCustom(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== 工具方法 =====

    private void stubLambdaQueryChain(List<Tag> result) {

        LambdaQueryChainWrapper<Tag> chain = MockChainHelper.mockChain();
        doReturn(chain).when(tagService).lambdaQuery();
        when(chain.list()).thenReturn(result);
    }

    /**
     * 多次 lambdaQuery 调用返回不同结果
     */
    @SuppressWarnings("unchecked")
    private void stubLambdaQueryChainSequential(List<Tag> first, List<Tag> second) {

        LambdaQueryChainWrapper<Tag> chain1 = MockChainHelper.mockChain();
        LambdaQueryChainWrapper<Tag> chain2 = MockChainHelper.mockChain();
        doReturn(chain1).doReturn(chain2).when(tagService).lambdaQuery();
        when(chain1.list()).thenReturn(first);
        when(chain2.list()).thenReturn(second);
    }
}
