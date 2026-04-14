package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jianzj.mistake.hub.backend.dto.req.MistakeAddReq;
import com.jianzj.mistake.hub.backend.dto.req.MistakeUpdateReq;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.mapper.MistakeMapper;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <p>MistakeService 单元测试</p>
 *
 * @author jian.zhong
 * @since 2026-04-13
 */
@ExtendWith(MockitoExtension.class)
class MistakeServiceTest {

    @Mock
    private MistakeMapper mockMistakeMapper;

    @Mock
    private MistakeTagService mistakeTagService;

    @Mock
    private TagService tagService;

    @Mock
    private AccountService accountService;

    @Mock
    private ThreadStorageUtil threadStorageUtil;

    private MistakeService mistakeService;

    @BeforeEach
    void setUp() {

        mistakeService = spy(new MistakeService(
                mistakeTagService, tagService, accountService,
                threadStorageUtil
        ));
        ReflectionTestUtils.setField(mistakeService, "baseMapper", mockMistakeMapper);
    }

    // ===== add =====

    @Test
    void add_shouldSetImageFields() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        doReturn(true).when(mistakeService).save(any(Mistake.class));

        MistakeAddReq req = new MistakeAddReq();
        req.setTitle("测试题干");
        req.setTitleImageUrl("title.jpg");
        req.setAnswerImageUrl("answer.jpg");

        mistakeService.add(req);

        ArgumentCaptor<Mistake> captor = ArgumentCaptor.forClass(Mistake.class);
        verify(mistakeService).save(captor.capture());
        Mistake saved = captor.getValue();

        assertThat(saved.getTitleImageUrl()).isEqualTo("title.jpg");
        assertThat(saved.getAnswerImageUrl()).isEqualTo("answer.jpg");
        assertThat(saved.getReviewStage()).isEqualTo(0);
        assertThat(saved.getMasteryLevel()).isEqualTo(0);
        assertThat(saved.getAccountId()).isEqualTo(100L);
    }

    @Test
    void add_withTags_shouldCallSaveTagIds() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        doReturn(true).when(mistakeService).save(any(Mistake.class));

        MistakeAddReq req = new MistakeAddReq();
        req.setTitle("测试题干");
        req.setTagIds(List.of(1L, 2L));

        mistakeService.add(req);

        verify(mistakeTagService).saveTagIds(any(), eq(List.of(1L, 2L)));
    }

    @Test
    void add_saveFails_shouldThrow() {

        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);
        doReturn(false).when(mistakeService).save(any(Mistake.class));

        MistakeAddReq req = new MistakeAddReq();
        req.setTitle("测试题干");

        assertThatThrownBy(() -> mistakeService.add(req))
                .isInstanceOf(BaseException.class);
    }

    // ===== modify =====

    @Test
    void modify_imageUrlNonBlank_shouldUpdate() {

        when(threadStorageUtil.getCurAccountRole()).thenReturn("admin");

        Mistake existing = Mistake.builder()
                .id(1L).accountId(100L).status(1)
                .titleImageUrl("old.jpg").answerImageUrl("old.jpg")
                .build();

        stubLambdaQueryChain(List.of(existing));
        doReturn(true).when(mistakeService).updateById(any(Mistake.class));

        MistakeUpdateReq req = new MistakeUpdateReq();
        req.setId(1L);
        req.setTitleImageUrl("new.jpg");
        req.setAnswerImageUrl("new.jpg");

        mistakeService.modify(req);

        ArgumentCaptor<Mistake> captor = ArgumentCaptor.forClass(Mistake.class);
        verify(mistakeService).updateById(captor.capture());
        Mistake updated = captor.getValue();

        assertThat(updated.getTitleImageUrl()).isEqualTo("new.jpg");
        assertThat(updated.getAnswerImageUrl()).isEqualTo("new.jpg");
    }

    @Test
    void modify_imageUrlEmpty_shouldClearToNull() {

        when(threadStorageUtil.getCurAccountRole()).thenReturn("admin");

        Mistake existing = Mistake.builder()
                .id(1L).accountId(100L).status(1)
                .titleImageUrl("old.jpg").answerImageUrl("old.jpg")
                .build();

        stubLambdaQueryChain(List.of(existing));
        doReturn(true).when(mistakeService).updateById(any(Mistake.class));

        MistakeUpdateReq req = new MistakeUpdateReq();
        req.setId(1L);
        req.setTitleImageUrl("");
        req.setAnswerImageUrl("");

        mistakeService.modify(req);

        ArgumentCaptor<Mistake> captor = ArgumentCaptor.forClass(Mistake.class);
        verify(mistakeService).updateById(captor.capture());
        Mistake updated = captor.getValue();

        assertThat(updated.getTitleImageUrl()).isNull();
        assertThat(updated.getAnswerImageUrl()).isNull();
    }

    @Test
    void modify_imageUrlNull_shouldNotChange() {

        when(threadStorageUtil.getCurAccountRole()).thenReturn("admin");

        Mistake existing = Mistake.builder()
                .id(1L).accountId(100L).status(1)
                .titleImageUrl("old.jpg").answerImageUrl("old.jpg")
                .build();

        stubLambdaQueryChain(List.of(existing));
        doReturn(true).when(mistakeService).updateById(any(Mistake.class));

        MistakeUpdateReq req = new MistakeUpdateReq();
        req.setId(1L);
        // 图片字段都不传（null）

        mistakeService.modify(req);

        ArgumentCaptor<Mistake> captor = ArgumentCaptor.forClass(Mistake.class);
        verify(mistakeService).updateById(captor.capture());
        Mistake updated = captor.getValue();

        assertThat(updated.getTitleImageUrl()).isEqualTo("old.jpg");
        assertThat(updated.getAnswerImageUrl()).isEqualTo("old.jpg");
    }

    // ===== listPage (D-02 回归) =====

    @Test
    @SuppressWarnings("unchecked")
    void listPage_adminNoAccountId_shouldFilterByCurAccount() {

        when(threadStorageUtil.getCurAccountRole()).thenReturn("admin");
        when(threadStorageUtil.getCurAccountId()).thenReturn(100L);

        LambdaQueryChainWrapper<Mistake> chain = mockChain();
        doReturn(chain).when(mistakeService).lambdaQuery();

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Mistake> emptyPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 0);
        emptyPage.setRecords(List.of());
        when(chain.page(any())).thenReturn(emptyPage);

        mistakeService.listPage(null, null, null, 1L, 10L);

        // D-02：admin + accountId=null 时必须调用 getCurAccountId() 按自己 ID 过滤
        verify(threadStorageUtil).getCurAccountId();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listPage_student_alwaysFiltersByCurAccount() {

        when(threadStorageUtil.getCurAccountRole()).thenReturn("student");
        when(threadStorageUtil.getCurAccountId()).thenReturn(200L);

        LambdaQueryChainWrapper<Mistake> chain = mockChain();
        doReturn(chain).when(mistakeService).lambdaQuery();

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Mistake> emptyPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 0);
        emptyPage.setRecords(List.of());
        when(chain.page(any())).thenReturn(emptyPage);

        mistakeService.listPage(null, null, null, 1L, 10L);

        verify(threadStorageUtil).getCurAccountId();
    }

    // ===== 工具方法 =====

    /**
     * 构建 LambdaQueryChainWrapper mock：所有返回类型兼容的方法自动返回 self
     */
    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<Mistake> mockChain() {

        return mock(LambdaQueryChainWrapper.class, invocation -> {
            if (invocation.getMethod().getReturnType().isAssignableFrom(invocation.getMock().getClass())) {
                return invocation.getMock();
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    /**
     * stub lambdaQuery 链式调用，最终返回给定列表
     */
    private void stubLambdaQueryChain(List<Mistake> result) {

        LambdaQueryChainWrapper<Mistake> chain = mockChain();
        doReturn(chain).when(mistakeService).lambdaQuery();
        when(chain.list()).thenReturn(result);
    }
}
