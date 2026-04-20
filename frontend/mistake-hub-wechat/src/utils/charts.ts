/**
 * 纯 SVG / View 驱动的图表辅助，不依赖 weapp 原生 Canvas。
 * 原生 Canvas 2d 在滚动时有 native layer 位置错位的 bug，改走 CSS 渲染规避。
 */

/**
 * 柱状图数据点：value 为 0-1 的完成率，null 表示当天无数据
 */
export interface BarPoint {
  value: number | null
}

/**
 * 折线图数据点：value 为 0-100 的掌握度，null 表示当天无数据（断线）
 */
export interface LinePoint {
  value: number | null
}

/**
 * 构造折线图 SVG 字符串（供 background-image 使用）。
 * 只渲染连线；圆点由 Tsx 用 View 绝对定位（避免 preserveAspectRatio 扭曲、且支持选中态）。
 */
export const buildLineChartSvg = (opts: {
  data: LinePoint[]
  lineColor: string
}): string => {

  const { data, lineColor } = opts
  const W = 300
  const H = 100
  const n = data.length

  if (n === 0) {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none"/>`
  }

  const step = n > 1 ? W / (n - 1) : 0
  const posX = (i: number) => (n > 1 ? i * step : W / 2)
  const posY = (v: number) => H - (Math.max(0, Math.min(100, v)) / 100) * H

  // 非空段落组装 path，null 断线
  const pathParts: string[] = []
  let started = false
  for (let i = 0; i < n; i++) {
    const v = data[i].value
    if (v == null) { started = false; continue }
    const x = posX(i).toFixed(2)
    const y = posY(v).toFixed(2)
    pathParts.push(started ? `L${x} ${y}` : `M${x} ${y}`)
    started = true
  }

  if (pathParts.length < 2) {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none"/>`
  }

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none"><path d="${pathParts.join(' ')}" stroke="${lineColor}" stroke-width="1.5" fill="none" stroke-linejoin="round" vector-effect="non-scaling-stroke"/></svg>`
}

/**
 * 把 SVG 字符串转成 background-image 可直接用的 data URL。
 * 用 URL 编码而非 base64，weapp 兼容性更好。
 */
export const svgToBgUrl = (svg: string): string => {

  const encoded = encodeURIComponent(svg)
    .replace(/'/g, '%27')
    .replace(/"/g, '%22')
  return `url("data:image/svg+xml;charset=utf-8,${encoded}")`
}
