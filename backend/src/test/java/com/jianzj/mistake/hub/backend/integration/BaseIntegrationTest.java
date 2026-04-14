package com.jianzj.mistake.hub.backend.integration;

import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.AccountService;
import com.jianzj.mistake.hub.backend.service.SessionService;
import com.jianzj.mistake.hub.backend.service.NonceService;
import com.jianzj.mistake.hub.backend.service.MistakeService;
import com.jianzj.mistake.hub.backend.service.ReviewPlanService;
import com.jianzj.mistake.hub.backend.service.ReviewRecordService;
import com.jianzj.mistake.hub.backend.service.ReviewScheduleService;
import com.jianzj.mistake.hub.backend.service.SystemConfigService;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * <p>集成测试基类：真实 DB + Redis，每个测试自动回滚</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected SessionService sessionService;

    @Autowired
    protected NonceService nonceService;

    @Autowired
    protected MistakeService mistakeService;

    @Autowired
    protected ReviewPlanService reviewPlanService;

    @Autowired
    protected ReviewRecordService reviewRecordService;

    @Autowired
    protected ReviewScheduleService reviewScheduleService;

    @Autowired
    protected SystemConfigService systemConfigService;

    @Autowired
    protected EncryptionUtil encryptionUtil;

    @Autowired
    protected ThreadStorageUtil threadStorageUtil;

    @AfterEach
    void clearThreadStorage() {

        threadStorageUtil.clear();
    }

    /**
     * 创建测试用户并返回
     */
    protected Account createTestAccount(Role role) {

        Account account = Account.builder()
                .code("test_" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("测试用户")
                .role(role.getCode())
                .build();

        accountService.save(account);
        return account;
    }

    /**
     * 模拟登录：设置 ThreadStorage
     */
    protected void loginAs(Account account) {

        threadStorageUtil.setCurAccountId(account.getId());
        threadStorageUtil.setCurAccountCode(account.getCode());
        threadStorageUtil.setCurAccountRole(account.getRole());
    }
}
