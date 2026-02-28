package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.Nonce;
import com.jianzj.mistake.hub.backend.mapper.NonceMapper;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 随机数 服务实现类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Service
@Slf4j
public class NonceService extends ServiceImpl<NonceMapper, Nonce> {

    private final EncryptionUtil encryptionUtil;

    public NonceService(EncryptionUtil encryptionUtil) {

        this.encryptionUtil = encryptionUtil;
    }

    /**
     * 生成 Nonce 并存入 DB
     */
    public Nonce generate() {

        Nonce nonce = Nonce.builder()
                .nonce(encryptionUtil.generateAesKey())
                .build();

        boolean success = save(nonce);
        if (!success) {
            oops("Nonce 生成失败", "Failed to generate nonce.");
        }

        return nonce;
    }

    /**
     * 根据 ID 获取 Nonce 并立即删除（即拿即删）
     */
    public Nonce consumeById(Long id) {

        Nonce nonce = getById(id);
        if (nonce == null) {
            oops("Nonce 不存在或已被使用", "Nonce does not exist or has been consumed.");
        }

        boolean success = removeById(id);
        if (!success) {
            oops("Nonce 删除失败", "Failed to remove nonce.");
        }

        return nonce;
    }
}
