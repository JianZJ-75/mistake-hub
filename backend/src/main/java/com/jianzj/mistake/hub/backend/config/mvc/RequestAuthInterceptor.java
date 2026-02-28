package com.jianzj.mistake.hub.backend.config.mvc;

import com.alibaba.fastjson2.JSON;
import com.jianzj.mistake.hub.backend.config.AuthCheckProperties;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Session;
import com.jianzj.mistake.hub.backend.service.AccountService;
import com.jianzj.mistake.hub.backend.service.SessionService;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import com.jianzj.mistake.hub.common.convention.result.BaseResult;
import com.jianzj.mistake.hub.common.utils.ThreadStorageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 认证、授权 拦截器
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Component
@Slf4j
public class RequestAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ThreadStorageUtil threadStorageUtil;

    private final EncryptionUtil encryptionUtil;

    private final AuthCheckProperties authCheckProperties;

    private final AccountService accountService;

    private final SessionService sessionService;

    private final int managementPort;

    public RequestAuthInterceptor(ThreadStorageUtil threadStorageUtil,
                                  EncryptionUtil encryptionUtil,
                                  AuthCheckProperties authCheckProperties,
                                  AccountService accountService,
                                  SessionService sessionService,
                                  @Value("${management.server.port}") int managementPort) {

        this.threadStorageUtil = threadStorageUtil;
        this.encryptionUtil = encryptionUtil;
        this.authCheckProperties = authCheckProperties;
        this.accountService = accountService;
        this.sessionService = sessionService;
        this.managementPort = managementPort;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        try {
            /**
             * 管理端口（Swagger 页面）不需要认证
             */
            if (request.getServerPort() == managementPort) {
                return true;
            }

            /**
             * 白名单路径不需要认证
             */
            String requestUri = request.getRequestURI();
            List<String> excludePathList = authCheckProperties.getExcludePathList();
            if (CollectionUtils.isNotEmpty(excludePathList)) {
                boolean excluded = excludePathList.stream()
                        .anyMatch(pattern -> PATH_MATCHER.match(pattern, requestUri));
                if (excluded) {
                    return true;
                }
            }

            String authorization = request.getHeader("Authorization");

            /**
             * 必须有 Authorization header
             */
            if (StringUtils.isBlank(authorization)) {
                make403(response, "认证失败：Authorization header 为空", "Authentication failed: Authorization header is empty.");
                return false;
            }

            /**
             * 必须是 Bearer
             */
            if (!authorization.startsWith("Bearer ")) {
                make403(response, "认证失败：仅支持 Bearer", "Authentication failed: only Bearer is supported.");
                return false;
            }

            Session session = getSessionByBearer(authorization);

            /**
             * session 不存在或已过期
             */
            if (session == null) {
                make403(response, "认证失败：该 token 不存在关联 session", "Authentication failed: The token does not have an associated session.");
                return false;
            }

            /**
             * 校验 session 关联的用户是否存在
             */
            Account account = accountService.getById(session.getAccountId());

            if (account == null) {
                make403(response, "认证失败：该 token 关联的用户不存在", "Authentication failed: The user associated with the token does not exist.");
                return false;
            }

            /**
             * 赋值 threadStorage
             */
            threadStorageUtil.setCurAccountId(account.getId());
            threadStorageUtil.setCurAccountCode(account.getCode());

            /**
             * 刷新 session 有效期
             */
            sessionService.renew(session);
            return true;

        } catch (Throwable e) {
            log.error("fail to process token check, error: ", e);
            make403(response, "认证失败：未知错误", "Authentication failed: unknown error.");
            return false;
        }
    }

    /**
     * response 设置成 429
     */
    private void make429(HttpServletResponse response, String zhCnMessage, String enUsMessage, Object... args) throws IOException {

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        BaseResult<Object> result = BaseResult.builder()
                .code(-1)
                .message(Map.of(
                        "zh_CN", String.format(zhCnMessage, args),
                        "en_US", String.format(enUsMessage, args)
                ))
                .build();
        response.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * response 设置成 403
     */
    private void make403(HttpServletResponse response, String zhCnMessage, String enUsMessage) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        BaseResult<Object> result = BaseResult.builder()
                .code(-1)
                .message(Map.of(
                        "zh_CN", zhCnMessage,
                        "en_US", enUsMessage
                ))
                .build();
        response.getWriter().write(JSON.toJSONString(result));
    }

    /**
     * 根据 bearer 拿到 session
     */
    private Session getSessionByBearer(String header) {

        String sessionKey = header.substring("Bearer ".length());
        return sessionService.getBySessionKey(sessionKey);
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {

        threadStorageUtil.clear();
    }
}
