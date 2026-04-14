package com.jianzj.mistake.hub.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzj.mistake.hub.backend.annotation.OperationLogAnno;
import com.jianzj.mistake.hub.backend.annotation.PreAuthorize;
import com.jianzj.mistake.hub.backend.dto.req.AccountChangePasswordReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountChangeRoleReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWebReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWxReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountResetPasswordReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountModifyProfileReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountUpdateDailyLimitReq;
import com.jianzj.mistake.hub.backend.dto.resp.AccountChangeRoleResp;
import com.jianzj.mistake.hub.backend.dto.resp.AccountDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.AccountResetPasswordResp;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.manager.AccountManager;
import com.jianzj.mistake.hub.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final AccountManager accountManager;

    public AccountController(AccountService accountService,
                             AccountManager accountManager) {

        this.accountService = accountService;
        this.accountManager = accountManager;
    }

    // ==================== 通用接口 ====================

    /**
     * 获取当前用户详情
     */
    @Operation(summary = "获取当前用户详情")
    @PostMapping("/current-detail")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public AccountDetailResp currentDetail() {

        return accountService.currentDetail();
    }

    // ==================== 小程序接口 ====================

    /**
     * 微信小程序登录
     */
    @Operation(summary = "微信小程序登录")
    @PostMapping("/login-wx")
    public String loginWx(@RequestBody @Validated @NotNull AccountLoginWxReq req) {

        return accountService.loginWx(req);
    }

    /**
     * 修改个人资料
     */
    @Operation(summary = "修改个人资料")
    @PostMapping("/modify-profile")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void modifyProfile(@RequestBody @Validated @NotNull AccountModifyProfileReq req) {

        accountService.modifyProfile(req);
    }

    /**
     * 修改每日复习量
     */
    @Operation(summary = "修改每日复习量")
    @PostMapping("/modify-daily-limit")
    @PreAuthorize(requiredRole = Role.STUDENT)
    public void modifyDailyLimit(@RequestBody @Validated @NotNull AccountUpdateDailyLimitReq req) {

        accountService.modifyDailyLimit(req);
    }

    // ==================== 管理端接口 ====================

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public Page<AccountDetailResp> list(@RequestParam(value = "code", required = false) String code,
                                        @RequestParam(value = "nickname", required = false) String nickname,
                                        @RequestParam(value = "role", required = false) String role,
                                        @RequestParam(value = "pageNum") @NotNull @Min(1) Long pageNum,
                                        @RequestParam(value = "pageSize") @NotNull @Min(1) Long pageSize) {

        return accountManager.listWithStats(code, nickname, role, pageNum, pageSize);
    }

    /**
     * Web 管理端登录
     */
    @Operation(summary = "Web 管理端登录")
    @PostMapping("/login-web")
    public String loginWeb(@RequestBody @Validated @NotNull AccountLoginWebReq req) {

        return accountService.loginWeb(req);
    }

    /**
     * 修改用户角色
     */
    @Operation(summary = "修改用户角色")
    @PostMapping("/change-role")
    @PreAuthorize(requiredRole = Role.ADMIN)
    @OperationLogAnno(action = "changeRole", targetType = "ACCOUNT")
    public AccountChangeRoleResp changeRole(@RequestBody @Validated @NotNull AccountChangeRoleReq req) {

        return accountService.changeRole(req);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    @PreAuthorize(requiredRole = Role.ADMIN)
    @OperationLogAnno(action = "resetPassword", targetType = "ACCOUNT")
    public AccountResetPasswordResp resetPassword(@RequestBody @Validated @NotNull AccountResetPasswordReq req) {

        return accountService.resetPassword(req);
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    @PreAuthorize(requiredRole = Role.ADMIN)
    public void changePassword(@RequestBody @Validated @NotNull AccountChangePasswordReq req) {

        accountService.changePassword(req);
    }
}
