/**
 * Scholar Flow 设计系统颜色常量 — 供 TSX 动态 style 使用
 * Primary #2563EB | Secondary #0255C5 | Slate 色阶
 */

// 语义色
export const COLOR_PRIMARY = '#2563EB'
export const COLOR_SECONDARY = '#0255C5'
export const COLOR_SUCCESS = '#10B981'
export const COLOR_WARNING = '#F59E0B'
export const COLOR_DANGER = '#EF4444'

// 掌握度专用色
export const COLOR_MASTERY_HIGH = '#10B981'
export const COLOR_MASTERY_MID = '#F59E0B'
export const COLOR_MASTERY_LOW = '#EF4444'

// 掌握深度条
export const COLOR_DEPTH_TRACK = '#DBEAFE'
export const COLOR_DEPTH_FILL = '#3B82F6'

// 背景 & 轨道
export const COLOR_TRACK = '#E2E8F0'
export const COLOR_STAT_SKIPPED = '#CBD5E1'
export const COLOR_STAT_PURPLE = '#0255C5'

/** 条形颜色按 avgMastery 三段 */
export const getBarColor = (avgMastery: number): string => {
  if (avgMastery < 60) return COLOR_MASTERY_LOW
  if (avgMastery < 80) return COLOR_MASTERY_MID
  return COLOR_MASTERY_HIGH
}

/** 完成率柱状颜色三段 */
export const getCompletionColor = (rate: number | null): string => {
  if (rate === null || rate < 0.5) return COLOR_DANGER
  if (rate < 0.8) return COLOR_WARNING
  return COLOR_SUCCESS
}

/** 掌握度散点颜色三段 */
export const getMasteryDotColor = (avg: number): string => {
  if (avg < 60) return COLOR_MASTERY_LOW
  if (avg < 80) return COLOR_MASTERY_MID
  return COLOR_MASTERY_HIGH
}
