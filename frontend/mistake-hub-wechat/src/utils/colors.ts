/**
 * 蓝紫色系常量 — 供 TSX 动态 style 使用
 * Primary #4F86F7 | Secondary #646DD5 | Tertiary #AE56A8 | Neutral #71749E
 */

// 语义色
export const COLOR_PRIMARY = '#4F86F7'
export const COLOR_SECONDARY = '#646DD5'
export const COLOR_SUCCESS = '#43A047'
export const COLOR_WARNING = '#FF9800'
export const COLOR_DANGER = '#E53935'

// 掌握度专用色（红橙绿）
export const COLOR_MASTERY_HIGH = '#43A047'
export const COLOR_MASTERY_MID = '#FF9800'
export const COLOR_MASTERY_LOW = '#E53935'

// 背景 & 轨道
export const COLOR_TRACK = '#E0E2F0'
export const COLOR_STAT_SKIPPED = '#C5C7D6'
export const COLOR_STAT_PURPLE = '#646DD5'

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
