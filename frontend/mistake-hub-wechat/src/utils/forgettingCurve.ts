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
 */
const sampleDecay = (
  startRetention: number,
  stability: number,
  durationDays: number,
  samplesPerDay: number
): number[] => {

  const points: number[] = []
  const totalSamples = Math.max(1, Math.ceil(durationDays * samplesPerDay))
  for (let i = 0; i <= totalSamples; i++) {
    const t = (i / samplesPerDay)
    const r = startRetention * Math.exp(-t / stability)
    points.push(Math.max(0, Math.min(1, r)))
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
  const predEndTs = toTs(prediction.fromTime) + prediction.daysToShow * 86400000
  const totalMs = predEndTs - timeOrigin
  const totalDays = totalMs / 86400000

  const toX = (ts: number) => ((ts - timeOrigin) / totalMs) * W
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

    const startR = 1.0
    const samples = sampleDecay(startR, seg.stability, durationDays, 2)

    for (let j = 0; j < samples.length; j++) {
      const t = j / 2
      const ts = segStartTs + t * 86400000
      const x = toX(ts).toFixed(2)
      const y = toY(samples[j]).toFixed(2)
      solidParts.push(j === 0 && i === 0 ? `M${x} ${y}` : solidParts.length === 0 ? `M${x} ${y}` : `L${x} ${y}`)
    }

    if (i > 0 && seg.isCorrect !== null) {
      markers.push({
        xPct: ((segStartTs - timeOrigin) / totalMs) * 100,
        yPct: 0,
        isCorrect: seg.isCorrect,
        time: seg.startTime,
      })
    }
  }

  // 预测段（虚线）
  const predStartTs = toTs(prediction.fromTime)
  const predSamples = sampleDecay(prediction.fromRetention, prediction.stability, prediction.daysToShow, 2)

  for (let j = 0; j < predSamples.length; j++) {
    const t = j / 2
    const ts = predStartTs + t * 86400000
    const x = toX(ts).toFixed(2)
    const y = toY(predSamples[j]).toFixed(2)
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

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none">${refLine}${solidPath}${dashPath}</svg>`

  return {
    svgBgUrl: svgToBgUrl(svg),
    reviewMarkers: markers,
    totalDays,
  }
}
