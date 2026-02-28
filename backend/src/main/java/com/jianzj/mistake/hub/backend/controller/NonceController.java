package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.entity.Nonce;
import com.jianzj.mistake.hub.backend.service.NonceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 随机数 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Tag(name = "随机数")
@RestController
@RequestMapping("/v1/nonce")
@Validated
@Slf4j
public class NonceController {

    private final NonceService nonceService;

    public NonceController(NonceService nonceService) {

        this.nonceService = nonceService;
    }

    /**
     * 生成 Nonce
     */
    @Operation(summary = "生成 Nonce")
    @PostMapping("/generate")
    public Nonce generate() {

        return nonceService.generate();
    }

    /**
     * 根据 ID 获取 Nonce（即拿即删）
     */
    @Operation(summary = "根据 ID 获取 Nonce")
    @PostMapping("/get")
    public Nonce get(Long id) {

        return nonceService.consumeById(id);
    }
}
