package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.mapper.ReviewRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.jianzj.mistake.hub.common.convention.exception.BaseException.oops;

/**
 * <p>
 * 复习记录 服务类
 * </p>
 *
 * @author jian.zhong
 * @since 2026-04-09
 */
@Service
@Slf4j
public class ReviewRecordService extends ServiceImpl<ReviewRecordMapper, ReviewRecord> {

    // ===== 业务方法 =====

    /**
     * 创建复习记录
     */
    public void createRecord(ReviewRecord record) {

        boolean success = save(record);
        if (!success) {
            oops("创建复习记录失败", "Failed to create review record.");
        }
    }

    // ===== 工具方法 =====
}
