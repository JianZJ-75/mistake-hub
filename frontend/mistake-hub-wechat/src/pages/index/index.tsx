import { useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Canvas } from '@tarojs/components'
import { reviewProgress } from '../../service/review'
import { ReviewProgressResp } from '../../types'
import './index.scss'

const IndexPage = () => {

  const [progress, setProgress] = useState<ReviewProgressResp | null>(null)
  const [loading, setLoading] = useState(true)

  useDidShow(() => {
    fetchProgress()
  })

  const fetchProgress = async () => {

    setLoading(true)
    try {
      const res = await reviewProgress()
      setProgress(res)
      drawRing(res)
    } catch {
      // request.ts 已处理 toast
    } finally {
      setLoading(false)
    }
  }

  const drawRing = (data: ReviewProgressResp) => {

    const query = Taro.createSelectorQuery()
    query.select('#progressCanvas')
      .fields({ node: true, size: true })
      .exec((results) => {
        if (!results || !results[0]) return
        const canvas = results[0].node
        if (!canvas) return
        const ctx = canvas.getContext('2d')
        const dpr = Taro.getSystemInfoSync().pixelRatio
        const width = results[0].width
        const height = results[0].height
        canvas.width = width * dpr
        canvas.height = height * dpr
        ctx.scale(dpr, dpr)

        const cx = width / 2
        const cy = height / 2
        const radius = Math.min(cx, cy) - 12
        const lineWidth = 10
        const startAngle = -Math.PI / 2

        const total = data.totalToday || 1
        const completedRatio = data.completedToday / total
        const skippedRatio = data.skippedToday / total

        // 底色（待复习）
        ctx.beginPath()
        ctx.arc(cx, cy, radius, 0, Math.PI * 2)
        ctx.strokeStyle = '#E3E8F0'
        ctx.lineWidth = lineWidth
        ctx.lineCap = 'round'
        ctx.stroke()

        // 已跳过（灰色），画在已完成之后
        if (skippedRatio > 0) {
          const skipStart = startAngle + Math.PI * 2 * completedRatio
          const skipEnd = skipStart + Math.PI * 2 * skippedRatio
          ctx.beginPath()
          ctx.arc(cx, cy, radius, skipStart, skipEnd)
          ctx.strokeStyle = '#C0C4CC'
          ctx.lineWidth = lineWidth
          ctx.lineCap = 'round'
          ctx.stroke()
        }

        // 已完成（绿色）
        if (completedRatio > 0) {
          const completedEnd = startAngle + Math.PI * 2 * completedRatio
          ctx.beginPath()
          ctx.arc(cx, cy, radius, startAngle, completedEnd)
          ctx.strokeStyle = '#4CAF50'
          ctx.lineWidth = lineWidth
          ctx.lineCap = 'round'
          ctx.stroke()
        }
      })
  }

  const pending = progress
    ? progress.totalToday - progress.completedToday - progress.skippedToday
    : 0

  const hasTask = progress && progress.totalToday > 0

  return (
    <View className='index-page'>
      {/* 进度环 */}
      <View className='ring-section'>
        <View className='ring-wrap'>
          <Canvas
            type='2d'
            id='progressCanvas'
            className='ring-canvas'
          />
          <View className='ring-inner'>
            {loading ? (
              <Text className='ring-loading'>加载中</Text>
            ) : hasTask ? (
              <>
                <Text className='ring-num'>{progress!.completedToday}/{progress!.totalToday}</Text>
                <Text className='ring-label'>已完成</Text>
              </>
            ) : (
              <Text className='ring-label'>今日无任务</Text>
            )}
          </View>
        </View>
      </View>

      {/* 数据卡片行 */}
      <View className='stats-row'>
        <View className='stat-card'>
          <Text className='stat-num stat-blue'>{pending}</Text>
          <Text className='stat-label'>待复习</Text>
        </View>
        <View className='stat-card'>
          <Text className='stat-num stat-green'>{progress?.completedToday ?? 0}</Text>
          <Text className='stat-label'>已完成</Text>
        </View>
        <View className='stat-card'>
          <Text className='stat-num stat-orange'>{progress?.streakDays ?? 0}</Text>
          <Text className='stat-label'>连续天数</Text>
        </View>
      </View>

      {/* 快速入口 */}
      <View className='action-section'>
        {hasTask ? (
          <View
            className='action-btn action-btn-primary'
            onClick={() => Taro.switchTab({ url: '/pages/review/index' })}
          >
            <Text className='action-btn-text'>开始复习</Text>
          </View>
        ) : (
          <View className='empty-hint-wrap'>
            <Text className='empty-hint-icon'>🎉</Text>
            <Text className='empty-hint-text'>今日无待复习错题，去录入新错题吧</Text>
          </View>
        )}
        <View
          className='action-btn action-btn-outline'
          onClick={() => Taro.navigateTo({ url: '/pages/mistake-add/index' })}
        >
          <Text className='action-btn-text-outline'>录入错题</Text>
        </View>
      </View>
    </View>
  )
}

export default IndexPage
