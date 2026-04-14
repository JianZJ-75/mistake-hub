import { useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { reviewProgress } from '../../service/review'
import { statsSubject } from '../../service/stats'
import { ReviewProgressResp, SubjectStatsResp } from '../../types'
import './index.scss'

/** 生成 conic-gradient 色段，用于纯 CSS 进度环 */
const buildConicGradient = (data: ReviewProgressResp): string => {

  const total = data.totalToday || 1
  const completedDeg = (data.completedToday / total) * 360
  const skippedDeg = (data.skippedToday / total) * 360

  // 从 12 点方向（-90deg）开始：已完成(绿) → 已跳过(灰) → 待复习(底色)
  const p1 = completedDeg
  const p2 = p1 + skippedDeg

  if (completedDeg === 0 && skippedDeg === 0) {
    return '#E3E8F0'
  }

  return `conic-gradient(from -90deg, #4CAF50 0deg ${p1}deg, #C0C4CC ${p1}deg ${p2}deg, #E3E8F0 ${p2}deg 360deg)`
}

/** 条形颜色按 avgMastery 三段 */
const getBarColor = (avgMastery: number): string => {

  if (avgMastery < 60) return '#F44336'
  if (avgMastery < 80) return '#FF9800'
  return '#4CAF50'
}

const IndexPage = () => {

  const [progress, setProgress] = useState<ReviewProgressResp | null>(null)
  const [subjectList, setSubjectList] = useState<SubjectStatsResp[]>([])
  const [loading, setLoading] = useState(true)

  useDidShow(() => {
    fetchProgress()
  })

  const fetchProgress = async () => {

    setLoading(true)
    try {
      const [res, subjects] = await Promise.all([reviewProgress(), statsSubject()])
      setProgress(res)
      setSubjectList(subjects || [])
    } catch {
      // request.ts 已处理 toast
    } finally {
      setLoading(false)
    }
  }

  const pending = progress
    ? progress.totalToday - progress.completedToday - progress.skippedToday
    : 0

  const hasTask = progress && progress.totalToday > 0

  const ringBg = progress ? buildConicGradient(progress) : '#E3E8F0'

  const maxCount = subjectList.length > 0 ? Math.max(...subjectList.map(s => s.count)) : 0

  return (
    <View className='index-page'>
      {/* 进度环（纯 CSS，避免 Canvas 原生组件层级问题） */}
      <View className='ring-section'>
        <View className='ring-wrap'>
          <View className='ring-outer' style={{ background: ringBg }} />
          <View className='ring-mask' />
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

      {/* 学科分布 */}
      <View className='subject-section'>
        <Text className='subject-title'>学科分布</Text>
        <View className='subject-card'>
          {subjectList.length > 0 ? (
            <>
              {subjectList.map(item => (
                <View className='subject-row' key={item.subject}>
                  <Text className='subject-label'>{item.subject}</Text>
                  <View className='subject-bar-bg'>
                    <View
                      className='subject-bar'
                      style={{
                        width: `${(item.count / maxCount) * 100}%`,
                        background: getBarColor(item.avgMastery)
                      }}
                    />
                  </View>
                  <Text className='subject-count'>{item.count}</Text>
                </View>
              ))}
              <View className='subject-legend'>
                <View className='subject-legend-item'>
                  <View className='subject-legend-dot' style={{ background: '#F44336' }} />
                  <Text className='subject-legend-text'>低掌握</Text>
                </View>
                <View className='subject-legend-item'>
                  <View className='subject-legend-dot' style={{ background: '#FF9800' }} />
                  <Text className='subject-legend-text'>中掌握</Text>
                </View>
                <View className='subject-legend-item'>
                  <View className='subject-legend-dot' style={{ background: '#4CAF50' }} />
                  <Text className='subject-legend-text'>高掌握</Text>
                </View>
              </View>
            </>
          ) : (
            <Text className='subject-empty'>暂无学科数据</Text>
          )}
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
