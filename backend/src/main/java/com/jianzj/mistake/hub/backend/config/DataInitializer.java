package com.jianzj.mistake.hub.backend.config;

import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.service.AccountService;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 数据初始化器，应用启动时初始化默认数据
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-27
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_CODE = "admin";

    private static final String ADMIN_DEFAULT_PASSWORD = "admin-123";

    private final AccountService accountService;

    private final EncryptionUtil encryptionUtil;

    public DataInitializer(AccountService accountService, EncryptionUtil encryptionUtil) {

        this.accountService = accountService;
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public void run(String... args) {

        initAdminAccount();
    }

    /**
     * 初始化管理员账户：不存在则创建，cipher_password 为空则生成
     */
    private void initAdminAccount() {

        Account admin = accountService.getByCodeAndRole(ADMIN_CODE, Role.ADMIN.getCode());

        if (admin == null) {
            admin = Account.builder()
                    .code(ADMIN_CODE)
                    .nickname("管理员")
                    .role(Role.ADMIN.getCode())
                    .dailyLimit(30L)
                    .build();
            boolean success = accountService.save(admin);
            if (!success) {
                oops("默认管理员账户创建失败", "Failed to create default admin account.");
            }
            log.info("已创建默认管理员账户 [{}]", ADMIN_CODE);
        }

        if (admin.getCipherPassword() != null) {
            return;
        }

        String digest = encryptionUtil.toDigest(ADMIN_DEFAULT_PASSWORD);
        String cipherPassword = encryptionUtil.encryptWithRawKey(digest, admin.getId());

        admin.setCipherPassword(cipherPassword);
        boolean success = accountService.updateById(admin);
        if (!success) {
            oops("管理员密码初始化失败", "Failed to initialize admin password.");
        }
        log.info("已为管理员账户 [{}] 初始化密码", ADMIN_CODE);
    }
}
