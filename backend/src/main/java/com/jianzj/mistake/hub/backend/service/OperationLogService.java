package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.req.LogListReq;
import com.jianzj.mistake.hub.backend.entity.OperationLog;
import com.jianzj.mistake.hub.backend.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 操作日志 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
@Service
@Slf4j
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {

    // ===== 业务方法 =====

    /**
     * 分页查询操作日志
     */
    public Page<OperationLog> listPage(LogListReq req) {

        return lambdaQuery()
                .eq(req.getAccountId() != null, OperationLog::getAccountId, req.getAccountId())
                .eq(req.getAction() != null, OperationLog::getAction, req.getAction())
                .ge(req.getStartTime() != null, OperationLog::getCreatedTime, req.getStartTime())
                .le(req.getEndTime() != null, OperationLog::getCreatedTime, req.getEndTime())
                .orderByDesc(OperationLog::getCreatedTime)
                .page(new Page<>(req.getPageNum(), req.getPageSize()));
    }

    /**
     * 记录操作日志
     */
    public void record(OperationLog operationLog) {

        save(operationLog);
    }
}
