package com.jianzj.mistake.hub.backend.controller;

import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWxReq;
import com.jianzj.mistake.hub.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/v1/account")
@Validated
@Slf4j
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 注册用户
     */
    @Operation(summary = "注册用户")
    @PostMapping("/register")
    public void register() {

        accountService.register();
    }

    /**
     * 删除指定用户
     */
    @Operation(summary = "删除指定用户")
    @PostMapping("/delete")
    public void delete() {

        accountService.delete();
    }

    /**
     * 登录微信小程序
     */
    @Operation(summary = "登录微信小程序")
    @PostMapping("/login-wx")
    public String loginWx(@RequestBody @Valid AccountLoginWxReq req) {

        return accountService.loginWx(req);
    }

    /**
     * 登录 Web 管理端
     */
    @Operation(summary = "登录 Web 管理端")
    @PostMapping("/login-web")
    public void loginWeb() {

        accountService.loginWeb();
    }
}
