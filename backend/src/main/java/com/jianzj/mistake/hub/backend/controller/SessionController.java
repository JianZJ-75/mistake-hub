package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.service.SessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户 Session 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Tag(name = "用户 Session")
@RestController
@RequestMapping("/v1/session")
@Validated
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }
}
