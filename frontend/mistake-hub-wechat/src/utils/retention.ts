/**
 * 记忆保持率计算工具（基于艾宾浩斯遗忘曲线 R(t) = e^(-t/S)）
 */

const DEFAULT_INTERVALS = [0, 1, 2, 4, 7, 15, 30]

const STABILITY_FACTOR = 1.44

/**
 * 计算当前记忆保持率
 * @param lastReviewTime 上次复习时间 ISO 字符串
 * @param stage 当前复习阶段 0-6
 * @returns 保持率 0~1
 */
export const calcRetention = (lastReviewTime: string | undefined, stage: number): number => {

  if (!lastReviewTime) return 1.0
  const t = (Date.now() - new Date(lastReviewTime).getTime()) / (1000 * 60 * 60 * 24)
  if (t < 0) return 1.0
  const interval = DEFAULT_INTERVALS[Math.min(stage, 6)]
  const S = interval > 0 ? interval * STABILITY_FACTOR : STABILITY_FACTOR
  return Math.max(0, Math.min(1, Math.exp(-t / S)))
}

/**
 * 根据保持率返回颜色
 */
export const getRetentionColor = (r: number): string => {

  if (r < 0.3) return '#EF4444'
  if (r < 0.7) return '#F59E0B'
  return '#10B981'
}

/**
 * 根据保持率返回文字标签
 */
export const getRetentionLabel = (r: number): string => {

  if (r < 0.3) return '记忆模糊'
  if (r < 0.7) return '记忆减退'
  return '记忆清晰'
}

/**
 * 根据阶段获取记忆稳定性
 */
export const calcStability = (stage: number): number => {

  const interval = DEFAULT_INTERVALS[Math.min(stage, 6)]
  return interval > 0 ? interval * STABILITY_FACTOR : STABILITY_FACTOR
}

export { DEFAULT_INTERVALS, STABILITY_FACTOR }
