package com.jianzj.mistake.hub.backend.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.config.WechatProperties;
import com.jianzj.mistake.hub.backend.dto.req.AccountChangePasswordReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountChangeRoleReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWebReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWxReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountResetPasswordReq;
import com.jianzj.mistake.hub.backend.dto.req.AccountUpdateDailyLimitReq;
import com.jianzj.mistake.hub.backend.dto.resp.AccountChangeRoleResp;
import com.jianzj.mistake.hub.backend.dto.resp.AccountDetailResp;
import com.jianzj.mistake.hub.backend.dto.resp.AccountResetPasswordResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Nonce;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.mapper.AccountMapper;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Service
@Slf4j
public class AccountService extends ServiceImpl<AccountMapper, Account> {

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private static final String DEFAULT_AVATAR = "https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI9FhqR54LN";

    private static final String DEFAULT_NAME = "微信用户";

    private final WechatProperties wechatProperties;

    private final SessionService sessionService;

    private final NonceService nonceService;

    private final EncryptionUtil encryptionUtil;

    private final ThreadStorageUtil threadStorageUtil;

    public AccountService(WechatProperties wechatProperties,
                          SessionService sessionService,
                          NonceService nonceService,
                          EncryptionUtil encryptionUtil,
                          ThreadStorageUtil threadStorageUtil) {

        this.wechatProperties = wechatProperties;
        this.sessionService = sessionService;
        this.nonceService = nonceService;
        this.encryptionUtil = encryptionUtil;
        this.threadStorageUtil = threadStorageUtil;
    }

    // ==================== 业务方法 ====================

    /**
     * 微信小程序登录
     */
    public String loginWx(AccountLoginWxReq req) {

        String openId = requestWxOpenId(req.getCode());

        Account account = getByWechatOpenId(openId);

        if (account == null) {
            account = Account.builder()
                    .code("wx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                    .nickname(DEFAULT_NAME)
                    .avatarUrl(DEFAULT_AVATAR)
                    .role(Role.STUDENT.getCode())
                    .wechatOpenId(openId)
                    .build();

            boolean success = save(account);
            if (!success) {
                oops("用户创建失败", "Failed to create account.");
            }
        }

        return sessionService.create(account.getId());
    }

    /**
     * Web 管理端登录
     */
    public String loginWeb(AccountLoginWebReq req) {

        Nonce nonce = nonceService.consumeById(req.getNonceId());

        String userDigest = encryptionUtil.decryptWithRawKey(req.getCipherDigest(), nonce.getNonce());

        Account account = getByCode(req.getCode());
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        if (Role.isInvalid(account.getRole()) || Role.fromCode(account.getRole()) != Role.ADMIN) {
            oops("该用户不是管理员", "The account is not an admin.");
        }

        if (StringUtils.isBlank(account.getCipherPassword())) {
            oops("该用户未设置密码", "The account has no password.");
        }

        String dbDigest = encryptionUtil.decryptWithRawKey(account.getCipherPassword(), account.getId());

        if (!userDigest.equals(dbDigest)) {
            oops("密码错误", "Incorrect password.");
        }

        return sessionService.create(account.getId());
    }

    /**
     * 修改用户角色（升级为管理员 / 降级为学生）
     */
    public AccountChangeRoleResp changeRole(AccountChangeRoleReq req) {

        Account account = getById(req.getAccountId());
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        Role targetRole = Role.fromCode(req.getRole());
        if (targetRole == null) {
            oops("无效的角色", "Invalid role.");
        }

        Role currentRole = Role.fromCode(account.getRole());
        if (currentRole == targetRole) {
            oops("用户已是该角色", "Account already has this role.");
        }

        AccountChangeRoleResp.AccountChangeRoleRespBuilder respBuilder = AccountChangeRoleResp.builder();

        if (targetRole == Role.ADMIN) {
            if (req.getNonceId() == null) {
                oops("升级管理员需要提供 nonceId", "nonceId is required for upgrading to admin.");
            }

            Nonce nonce = nonceService.consumeById(req.getNonceId());

            String rawPassword = encryptionUtil.generatePwd();
            String digest = encryptionUtil.toDigest(rawPassword);
            account.setCipherPassword(encryptionUtil.encryptWithRawKey(digest, account.getId()));

            String encryptedPassword = encryptionUtil.encrypt(rawPassword, nonce.getNonce());
            respBuilder.cipherPassword(encryptedPassword);
        } else {
            account.setCipherPassword(null);
            sessionService.removeByAccountId(account.getId());
        }

        account.setRole(targetRole.getCode());

        boolean success = updateById(account);
        if (!success) {
            oops("修改用户角色失败", "Failed to change account role.");
        }

        return respBuilder.build();
    }

    /**
     * 重置密码
     */
    public AccountResetPasswordResp resetPassword(AccountResetPasswordReq req) {

        Account account = getById(req.getAccountId());
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        if (Role.fromCode(account.getRole()) != Role.ADMIN) {
            oops("该用户不是管理员，无密码可重置", "The account is not an admin.");
        }

        String rawPassword = encryptionUtil.generatePwd();
        String digest = encryptionUtil.toDigest(rawPassword);
        String cipherPassword = encryptionUtil.encryptWithRawKey(digest, account.getId());

        account.setCipherPassword(cipherPassword);

        boolean success = updateById(account);
        if (!success) {
            oops("重置密码失败", "Failed to reset password.");
        }

        sessionService.removeByAccountId(account.getId());

        Nonce nonce = nonceService.generate();
        String encryptedPassword = encryptionUtil.encrypt(rawPassword, nonce.getNonce());

        return AccountResetPasswordResp.builder()
                .nonceId(nonce.getId())
                .cipherPassword(encryptedPassword)
                .build();
    }

    /**
     * 修改密码
     */
    public void changePassword(AccountChangePasswordReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();
        Account account = getById(accountId);
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        if (StringUtils.isBlank(account.getCipherPassword())) {
            oops("该用户未设置密码", "The account has no password.");
        }

        Nonce oldNonce = nonceService.consumeById(req.getOldNonceId());
        String oldDigest = encryptionUtil.decryptWithRawKey(req.getOldCipherDigest(), oldNonce.getNonce());

        String dbDigest = encryptionUtil.decryptWithRawKey(account.getCipherPassword(), account.getId());
        if (!oldDigest.equals(dbDigest)) {
            oops("旧密码错误", "Old password is incorrect.");
        }

        Nonce newNonce = nonceService.consumeById(req.getNewNonceId());
        String newDigest = encryptionUtil.decryptWithRawKey(req.getNewCipherDigest(), newNonce.getNonce());

        String newCipherPassword = encryptionUtil.encryptWithRawKey(newDigest, account.getId());
        account.setCipherPassword(newCipherPassword);

        boolean success = updateById(account);
        if (!success) {
            oops("修改密码失败", "Failed to change password.");
        }

        sessionService.removeByAccountId(accountId);
    }

    /**
     * 分页查询用户列表，支持按 code/昵称关键字和角色筛选
     */
    public Page<AccountDetailResp> list(String code, String nickname, String role, Long pageNum, Long pageSize) {

        Page<Account> accountPage = lambdaQuery()
                .like(StringUtils.isNotBlank(code), Account::getCode, code)
                .like(StringUtils.isNotBlank(nickname), Account::getNickname, nickname)
                .eq(StringUtils.isNotBlank(role), Account::getRole, role)
                .page(new Page<>(pageNum, pageSize));

        List<AccountDetailResp> respList = accountPage.getRecords().stream()
                .map(this::toDetailResp)
                .collect(Collectors.toList());

        Page<AccountDetailResp> respPage = new Page<>(accountPage.getCurrent(), accountPage.getSize(), accountPage.getTotal());
        respPage.setRecords(respList);
        return respPage;
    }

    /**
     * 获取当前用户详情
     */
    public AccountDetailResp currentDetail() {

        Long accountId = threadStorageUtil.getCurAccountId();
        Account account = getById(accountId);
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        return toDetailResp(account);
    }

    /**
     * 修改每日复习量
     */
    public void updateDailyLimit(AccountUpdateDailyLimitReq req) {

        Long accountId = threadStorageUtil.getCurAccountId();
        Account account = getById(accountId);
        if (account == null) {
            oops("用户不存在", "Account does not exist.");
        }

        account.setDailyLimit(req.getDailyLimit());

        boolean success = updateById(account);
        if (!success) {
            oops("修改每日复习量失败", "Failed to update daily limit.");
        }
    }

    // ==================== 工具方法 ====================

    /** 将 Account 实体转为 AccountDetailResp */
    private AccountDetailResp toDetailResp(Account account) {

        AccountDetailResp resp = new AccountDetailResp();
        BeanUtils.copyProperties(account, resp);
        return resp;
    }

    /**
     * 根据微信 openId 查询用户
     */
    private Account getByWechatOpenId(String openId) {

        List<Account> accounts = lambdaQuery().eq(Account::getWechatOpenId, openId).list();
        return CollectionUtils.isEmpty(accounts) ? null : accounts.get(0);
    }

    /**
     * 根据用户 code 查询用户
     */
    private Account getByCode(String code) {

        List<Account> accounts = lambdaQuery().eq(Account::getCode, code).list();
        return CollectionUtils.isEmpty(accounts) ? null : accounts.get(0);
    }

    /**
     * 根据用户 code 和角色查询用户
     */
    public Account getByCodeAndRole(String code, String role) {

        List<Account> accounts = lambdaQuery()
                .eq(Account::getCode, code)
                .eq(Account::getRole, role)
                .list();
        return CollectionUtils.isEmpty(accounts) ? null : accounts.get(0);
    }

    /**
     * 请求微信服务器获取 openId
     */
    private String requestWxOpenId(String code) {

        Map<String, Object> params = new HashMap<>();
        params.put("appid", wechatProperties.getAppId());
        params.put("secret", wechatProperties.getAppSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String response = HttpUtil.get(WX_LOGIN_URL, params);
        JSONObject json = JSON.parseObject(response);

        String openId = json.getString("openid");
        if (StringUtils.isBlank(openId)) {
            int errCode = json.getIntValue("errcode");
            String errMsg = json.getString("errmsg");
            log.error("微信登录失败, errCode={}, errMsg={}", errCode, errMsg);
            oops("微信登录失败：%s", "WeChat login failed: %s", errMsg);
        }

        return openId;
    }
}
