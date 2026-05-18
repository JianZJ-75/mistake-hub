/**
 * 遗忘曲线 SVG 构建器
 * 复用 charts.ts 的 svgToBgUrl 模式，但渲染指数衰减曲线
 */

import { svgToBgUrl } from './charts'
import { CurveSegment, CurvePrediction } from '../types'

const W = 300
const H = 120

/**
 * 将时间字符串转为毫秒时间戳
 */
const toTs = (s: string): number => new Date(s).getTime()

/**
 * 指数衰减采样：给定起始保持率、稳定性、时间跨度，返回采样点
 * 最后一个采样点严格落在 durationDays 位置，避免越过段终点
 */
const sampleDecay = (
  startRetention: number,
  stability: number,
  durationDays: number,
  samplesPerDay: number
): { t: number; r: number }[] => {

  const points: { t: number; r: number }[] = []
  const totalSamples = Math.max(1, Math.ceil(durationDays * samplesPerDay))
  for (let i = 0; i <= totalSamples; i++) {
    const t = Math.min(i / samplesPerDay, durationDays)
    const r = startRetention * Math.exp(-t / stability)
    points.push({ t, r: Math.max(0, Math.min(1, r)) })
  }
  return points
}

export interface ForgettingCurveSvgResult {
  svgBgUrl: string
  reviewMarkers: ReviewMarker[]
  totalDays: number
}

export interface ReviewMarker {
  xPct: number
  yPct: number
  isCorrect: boolean | null
  time: string
}

/**
 * 构建遗忘曲线 SVG 和标记点
 * x 轴采用 50/50 分段映射：左半绘制实际段 [createdTime, now]，右半绘制预测段 [now, now + daysToShow]
 * 这样无论错题新旧，实际历史与未来预测都各占一半，视觉均衡
 */
export const buildForgettingCurveSvg = (opts: {
  segments: CurveSegment[]
  prediction: CurvePrediction
  createdTime: string
}): ForgettingCurveSvgResult => {

  const { segments, prediction, createdTime } = opts

  if (segments.length === 0) {
    const emptySvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none"/>`
    return { svgBgUrl: svgToBgUrl(emptySvg), reviewMarkers: [], totalDays: 1 }
  }

  const timeOrigin = toTs(createdTime)
  const predStartTs = toTs(prediction.fromTime)
  const predMs = Math.max(1, prediction.daysToShow * 86400000)
  const actualMs = Math.max(1, predStartTs - timeOrigin)

  // 50/50 分段映射：ts ≤ now 落在左半，ts > now 落在右半
  const toX = (ts: number): number => {
    if (ts <= predStartTs) {
      return ((ts - timeOrigin) / actualMs) * (W / 2)
    }
    return (W / 2) + ((ts - predStartTs) / predMs) * (W / 2)
  }
  const toY = (r: number) => H - r * H

  const solidParts: string[] = []
  const dashParts: string[] = []
  const markers: ReviewMarker[] = []

  for (let i = 0; i < segments.length; i++) {
    const seg = segments[i]
    const segStartTs = toTs(seg.startTime)
    const segEndTs = toTs(seg.endTime)
    const durationDays = (segEndTs - segStartTs) / 86400000

    if (durationDays <= 0) continue

    const startR = seg.startRetention ?? 1.0
    const samples = sampleDecay(startR, seg.stability, durationDays, 2)

    for (let j = 0; j < samples.length; j++) {
      const ts = segStartTs + samples[j].t * 86400000
      const x = toX(ts).toFixed(2)
      const y = toY(samples[j].r).toFixed(2)
      solidParts.push(solidParts.length === 0 ? `M${x} ${y}` : `L${x} ${y}`)
    }

    if (i > 0 && seg.isCorrect !== null) {
      markers.push({
        xPct: (toX(segStartTs) / W) * 100,
        yPct: (1 - (seg.startRetention ?? 1.0)) * 100,
        isCorrect: seg.isCorrect,
        time: seg.startTime,
      })
    }
  }

  // 预测段（虚线）：从 now 开始，daysToShow 天
  const predSamples = sampleDecay(prediction.fromRetention, prediction.stability, prediction.daysToShow, 2)

  for (let j = 0; j < predSamples.length; j++) {
    const ts = predStartTs + predSamples[j].t * 86400000
    const x = toX(ts).toFixed(2)
    const y = toY(predSamples[j].r).toFixed(2)
    dashParts.push(j === 0 ? `M${x} ${y}` : `L${x} ${y}`)
  }

  const solidPath = solidParts.length >= 2
    ? `<path d="${solidParts.join(' ')}" stroke="#2563EB" stroke-width="1.8" fill="none" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>`
    : ''

  const dashPath = dashParts.length >= 2
    ? `<path d="${dashParts.join(' ')}" stroke="#94A3B8" stroke-width="1.2" fill="none" stroke-dasharray="4 3" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>`
    : ''

  // 50% 参考线
  const refY = toY(0.5).toFixed(2)
  const refLine = `<line x1="0" y1="${refY}" x2="${W}" y2="${refY}" stroke="#E2E8F0" stroke-width="0.8" stroke-dasharray="3 3" vector-effect="non-scaling-stroke"/>`

  // 实际/预测分界线（轻灰竖线，标示 "now"）
  const dividerX = (W / 2).toFixed(2)
  const dividerLine = `<line x1="${dividerX}" y1="0" x2="${dividerX}" y2="${H}" stroke="#E2E8F0" stroke-width="0.6" stroke-dasharray="2 3" vector-effect="non-scaling-stroke"/>`

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none">${refLine}${dividerLine}${solidPath}${dashPath}</svg>`

  return {
    svgBgUrl: svgToBgUrl(svg),
    reviewMarkers: markers,
    totalDays: (actualMs + predMs) / 86400000,
  }
}
