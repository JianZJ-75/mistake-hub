package com.jianzj.mistake.hub.backend.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewRecordResp;
import com.jianzj.mistake.hub.backend.entity.ReviewRecord;
import com.jianzj.mistake.hub.backend.mapper.ReviewRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 查询某道错题的复习记录（按时间倒序）
     */
    public List<ReviewRecordResp> listByMistake(Long accountId, Long mistakeId) {

        List<ReviewRecord> records = lambdaQuery()
                .eq(ReviewRecord::getAccountId, accountId)
                .eq(ReviewRecord::getMistakeId, mistakeId)
                .orderByDesc(ReviewRecord::getReviewTime)
                .list();

        if (CollectionUtils.isEmpty(records)) {
            return new ArrayList<>();
        }

        return records.stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    // ===== 工具方法 =====

    /**
     * ReviewRecord -> ReviewRecordResp
     */
    private ReviewRecordResp toResp(ReviewRecord record) {

        ReviewRecordResp resp = new ReviewRecordResp();
        BeanUtils.copyProperties(record, resp);
        return resp;
    }
}
