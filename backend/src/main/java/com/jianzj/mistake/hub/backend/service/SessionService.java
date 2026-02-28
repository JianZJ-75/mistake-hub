package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.Session;
import com.jianzj.mistake.hub.backend.mapper.SessionMapper;
import com.jianzj.mistake.hub.backend.utils.encryption.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户 Session 服务实现类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-02-21
 */
@Service
@Slf4j
public class SessionService extends ServiceImpl<SessionMapper, Session> {

    private static final int SESSION_EXPIRE_DAYS = 1;

    private final EncryptionUtil encryptionUtil;

    public SessionService(EncryptionUtil encryptionUtil) {

        this.encryptionUtil = encryptionUtil;
    }

    /**
     * 创建 Session，返回 token
     */
    public String create(Long accountId) {

        Session session = Session.builder()
                .accountId(accountId)
                .token(encryptionUtil.generateKey())
                .expireTime(LocalDateTime.now().plusDays(SESSION_EXPIRE_DAYS))
                .build();
        boolean success = save(session);
        if (!success) {
            oops("Session 创建失败", "Failed to create session.");
        }
        return session.getToken();
    }

    /**
     * 根据 token 获取 Session，过期则删除返回 null
     */
    public Session getBySessionKey(String token) {

        Session session = getByToken(token);

        if (session == null) {
            return null;
        }

        if (session.getExpireTime().isBefore(LocalDateTime.now())) {
            boolean success = removeById(session.getId());
            if (!success) {
                oops("过期 Session 清理失败", "Failed to remove expired session.");
            }
            return null;
        }

        return session;
    }

    /**
     * 刷新 Session 过期时间
     */
    public void renew(Session session) {

        session.setExpireTime(LocalDateTime.now().plusDays(SESSION_EXPIRE_DAYS));
        boolean success = updateById(session);
        if (!success) {
            oops("Session 续期失败", "Failed to renew session.");
        }
    }

    /**
     * 删除指定用户的所有 Session
     */
    public void removeByAccountId(Long accountId) {

        lambdaUpdate().eq(Session::getAccountId, accountId).remove();
    }

    // ==================== 工具方法 ====================

    /**
     * 根据 token 查询 Session
     */
    private Session getByToken(String token) {

        List<Session> sessions = lambdaQuery().eq(Session::getToken, token).list();
        return CollectionUtils.isEmpty(sessions) ? null : sessions.get(0);
    }
}
