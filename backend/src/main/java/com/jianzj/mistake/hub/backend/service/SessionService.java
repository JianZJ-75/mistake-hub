package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.Session;
import com.jianzj.mistake.hub.backend.mapper.SessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
