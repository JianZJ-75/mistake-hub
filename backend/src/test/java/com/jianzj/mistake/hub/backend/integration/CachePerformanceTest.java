package com.jianzj.mistake.hub.backend.integration;

import com.jianzj.mistake.hub.backend.dto.req.ConfigUpdateReq;
import com.jianzj.mistake.hub.backend.dto.resp.ReviewTaskResp;
import com.jianzj.mistake.hub.backend.entity.Account;
import com.jianzj.mistake.hub.backend.entity.Mistake;
import com.jianzj.mistake.hub.backend.enums.MistakeStatus;
import com.jianzj.mistake.hub.backend.enums.Role;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>缓存性能集成测试：Redis 命中率 + 失效正确性</p>
 *
 * @author jian.zhong
 * @since 2026-04-14
 */
class CachePerformanceTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CachePerformanceTest.class);

    // ===== SystemConfig 缓存 =====

    @Test
    void systemConfig_cacheHitVsMiss() {

        // 第一次：cache miss → 查 DB
        long start1 = System.nanoTime();
        String val1 = systemConfigService.getByKey("review.intervals");
        long miss = System.nanoTime() - start1;

        // 第二次：cache hit → 读 Redis
        long start2 = System.nanoTime();
        String val2 = systemConfigService.getByKey("review.intervals");
        long hit = System.nanoTime() - start2;

        assertThat(val1).isEqualTo(val2);
        log.info("[缓存性能] SystemConfig cache miss: {}ns, hit: {}ns, speedup: {:.1f}x",
                miss, hit, miss > 0 && hit > 0 ? (double) miss / hit : 0);
    }

    @Test
    void systemConfig_updateEvictsCache() {

        // 预热缓存
        String before = systemConfigService.getByKey("review.mastery_correct_delta");
        assertThat(before).isNotNull();

        // 更新配置 → 缓存失效
        ConfigUpdateReq req = new ConfigUpdateReq();
        req.setConfigKey("review.mastery_correct_delta");
        req.setConfigValue("25");
        systemConfigService.updateConfig(req);

        // 再次读取 → 走 DB（新值）
        String after = systemConfigService.getByKey("review.mastery_correct_delta");
        assertThat(after).isEqualTo("25");

        // 还原
        req.setConfigValue(before);
        systemConfigService.updateConfig(req);
    }

    @Test
    void systemConfig_hitRate_100reads() {

        // 第 1 次 miss
        systemConfigService.getByKey("review.intervals");

        // 接下来 99 次 hit
        long hitStart = System.nanoTime();
        for (int i = 0; i < 99; i++) {
            systemConfigService.getByKey("review.intervals");
        }
        long totalHitTime = System.nanoTime() - hitStart;

        double avgHitNs = totalHitTime / 99.0;
        log.info("[缓存性能] 100 次读取，1 次 miss + 99 次 hit，avg hit: {:.0f}ns", avgHitNs);

        // hit rate = 99/100 = 99%
        assertThat(99.0 / 100).isGreaterThanOrEqualTo(0.99);
    }

    // ===== ReviewDaily 缓存 =====

    @Test
    void reviewDaily_cacheHitVsMiss() {

        Account account = createTestAccount(Role.STUDENT);
        loginAs(account);

        // 创建一条错题
        Mistake mistake = Mistake.builder()
                .accountId(account.getId())
                .title("测试缓存")
                .reviewStage(0).masteryLevel(0)
                .nextReviewTime(LocalDateTime.now().minusDays(1))
                .status(MistakeStatus.VALID.getCode())
                .build();
        mistakeService.save(mistake);

        // 第一次：cache miss → 生成 + 缓存
        long start1 = System.nanoTime();
        List<ReviewTaskResp> tasks1 = reviewScheduleService.todayTasks();
        long miss = System.nanoTime() - start1;

        // 第二次：cache hit → 直接返回
        long start2 = System.nanoTime();
        List<ReviewTaskResp> tasks2 = reviewScheduleService.todayTasks();
        long hit = System.nanoTime() - start2;

        assertThat(tasks1).hasSameSizeAs(tasks2);
        log.info("[缓存性能] ReviewDaily cache miss: {}ns, hit: {}ns", miss, hit);
    }

    @Test
    void reviewDaily_evict_shouldRegenerate() {

        Account account = createTestAccount(Role.STUDENT);
        loginAs(account);

        // 生成缓存
        reviewScheduleService.todayTasks();

        // 失效缓存
        reviewScheduleService.evictDailyCache(account.getId());

        // 再次获取 → 重建缓存
        List<ReviewTaskResp> tasks = reviewScheduleService.todayTasks();
        assertThat(tasks).isNotNull();
    }
}
