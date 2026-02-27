package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.service.NonceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
}
