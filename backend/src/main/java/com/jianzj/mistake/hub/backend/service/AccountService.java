package com.jianzj.mistake.hub.backend.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.config.WechatProperties;
import com.jianzj.mistake.hub.backend.dto.req.AccountLoginWxReq;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.enums.Role;
import com.jianzj.mistake.hub.backend.mapper.AccountMapper;
import com.jianzj.mistake.hub.common.convention.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public AccountService(WechatProperties wechatProperties) {

        this.wechatProperties = wechatProperties;
    }

    /**
     * 微信小程序登录
     */
    public String loginWx(AccountLoginWxReq req) {

        String openId = requestWxOpenId(req.getCode());

        Account account = getOne(
                new LambdaQueryWrapper<Account>().eq(Account::getWechatOpenId, openId)
        );

        if (account == null) {
            account = Account.builder()
                    .code("wx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                    .name(DEFAULT_NAME)
                    .avatar(DEFAULT_AVATAR)
                    .role(Role.STUDENT.getCode())
                    .wechatOpenId(openId)
                    .build();
            save(account);
        }

        return "ok";
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
            BaseException.oops("微信登录失败：%s", "WeChat login failed: %s", errMsg);
        }

        return openId;
    }
}
