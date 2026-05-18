import { useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { reviewProgress } from '../../service/review'
import { statsSubject, statsMemoryHealth } from '../../service/stats'
import { ReviewProgressResp, SubjectStatsResp, MemoryHealthResp } from '../../types'
import { COLOR_PRIMARY, COLOR_TRACK, COLOR_MASTERY_HIGH, COLOR_MASTERY_MID, COLOR_MASTERY_LOW, getBarColor } from '../../utils/colors'
import { getRetentionColor } from '../../utils/retention'
import './index.scss'

/** 生成 conic-gradient 色段，用于纯 CSS 进度环 */
const buildConicGradient = (data: ReviewProgressResp): string => {

  const total = data.totalToday || 1
  const completedDeg = (data.completedToday / total) * 360

  if (completedDeg === 0) {
    return COLOR_TRACK
  }

  return `conic-gradient(from -90deg, ${COLOR_PRIMARY} 0deg ${completedDeg}deg, ${COLOR_TRACK} ${completedDeg}deg 360deg)`
}

/** 根据进度生成激励文案 */
const getMotivation = (completed: number, total: number): string => {

  if (total === 0) return ''
  const rate = completed / total
  if (rate >= 1) return '太棒了！今天的任务全部完成！'
  if (rate >= 0.5) return '今天你的进步很稳健！'
  if (rate > 0) return '加油！继续保持！'
  return '新的一天，开始复习吧！'
}

const IndexPage = () => {

  const [progress, setProgress] = useState<ReviewProgressResp | null>(null)
  const [subjectList, setSubjectList] = useState<SubjectStatsResp[]>([])
  const [health, setHealth] = useState<MemoryHealthResp | null>(null)
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
      statsMemoryHealth().then(setHealth).catch(() => {})
    } catch {
      // request.ts 已处理 toast
    } finally {
      setLoading(false)
    }
  }

  const pending = progress
    ? progress.totalToday - progress.completedToday
    : 0

  const hasTask = progress && progress.totalToday > 0
  const ringBg = progress ? buildConicGradient(progress) : COLOR_TRACK
  const maxCount = subjectList.length > 0 ? Math.max(...subjectList.map(s => s.count)) : 0
  const motivation = progress ? getMotivation(progress.completedToday, progress.totalToday) : ''

  return (
    <View className='index-page'>
      {/* 进度环 */}
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

      {/* 激励文案 */}
      {motivation && <Text className='motivation-text'>{motivation}</Text>}

      {/* 数据卡片行 */}
      <View className='stats-row'>
        <View className='stat-card'>
          <View className='stat-icon stat-icon-blue'>
            <Text className='stat-icon-text'>📋</Text>
          </View>
          <Text className='stat-num stat-blue'>{pending}</Text>
          <Text className='stat-label'>待复习</Text>
        </View>
        <View className='stat-card'>
          <View className='stat-icon stat-icon-green'>
            <Text className='stat-icon-text'>✓</Text>
          </View>
          <Text className='stat-num stat-green'>{progress?.completedToday ?? 0}</Text>
          <Text className='stat-label'>已完成</Text>
        </View>
        <View className='stat-card'>
          <View className='stat-icon stat-icon-orange'>
            <Text className='stat-icon-text'>🏠</Text>
          </View>
          <Text className='stat-num stat-orange'>{progress?.streakDays ?? 0}</Text>
          <Text className='stat-label'>连续天数</Text>
        </View>
      </View>

      {/* 记忆健康 */}
      {health && health.totalActive > 0 && (
        <View className='health-section'>
          <Text className='health-title'>记忆健康</Text>
          <View className='health-card'>
            <View className='health-gauge-wrap'>
              <View className='health-gauge'>
                <View className='health-gauge-ring' style={{
                  background: `conic-gradient(from -270deg, ${getRetentionColor(health.overallRetention)} 0deg ${health.overallRetention * 180}deg, ${COLOR_TRACK} ${health.overallRetention * 180}deg 180deg, transparent 180deg 360deg)`
                }} />
                <View className='health-gauge-mask' />
                <View className='health-gauge-label'>
                  <Text className='health-gauge-pct' style={{ color: getRetentionColor(health.overallRetention) }}>
                    {Math.round(health.overallRetention * 100)}%
                  </Text>
                  <Text className='health-gauge-hint'>平均记忆保持率</Text>
                </View>
              </View>
            </View>

            <View className='health-zones'>
              <View className='health-zone-item'>
                <View className='health-zone-dot' style={{ background: COLOR_MASTERY_LOW }} />
                <Text className='health-zone-count'>{health.dangerCount}</Text>
                <Text className='health-zone-label'>记忆模糊</Text>
              </View>
              <View className='health-zone-item'>
                <View className='health-zone-dot' style={{ background: COLOR_MASTERY_MID }} />
                <Text className='health-zone-count'>{health.warningCount}</Text>
                <Text className='health-zone-label'>记忆减退</Text>
              </View>
              <View className='health-zone-item'>
                <View className='health-zone-dot' style={{ background: COLOR_MASTERY_HIGH }} />
                <Text className='health-zone-count'>{health.safeCount}</Text>
                <Text className='health-zone-label'>记忆清晰</Text>
              </View>
            </View>

            <Text className='health-forecast-title'>未来 7 天复习预报</Text>
            <View className='health-forecast-row'>
              {health.upcoming.map((item, idx) => {
                const maxCount = Math.max(...health.upcoming.map(u => u.count), 1)
                const barH = Math.max(4, (item.count / maxCount) * 80)
                const dayLabel = idx === 0 ? '今' : idx === 1 ? '明' : `+${idx}`
                return (
                  <View key={idx} className='health-forecast-col'>
                    <Text className='health-forecast-num'>{item.count}</Text>
                    <View className='health-forecast-bar' style={{ height: `${barH}rpx`, background: COLOR_PRIMARY }} />
                    <Text className='health-forecast-day'>{dayLabel}</Text>
                  </View>
                )
              })}
            </View>
          </View>
        </View>
      )}

      {/* 学科分布 */}
      <View className='subject-section'>
        <View className='subject-header'>
          <Text className='subject-title'>学科分布</Text>
          <View className='subject-legend'>
            <View className='subject-legend-item'>
              <View className='subject-legend-dot' style={{ background: COLOR_MASTERY_LOW }} />
              <Text className='subject-legend-text'>低掌握</Text>
            </View>
            <View className='subject-legend-item'>
              <View className='subject-legend-dot' style={{ background: COLOR_MASTERY_MID }} />
              <Text className='subject-legend-text'>中掌握</Text>
            </View>
            <View className='subject-legend-item'>
              <View className='subject-legend-dot' style={{ background: COLOR_MASTERY_HIGH }} />
              <Text className='subject-legend-text'>高掌握</Text>
            </View>
          </View>
        </View>
        <View className='subject-card'>
          {subjectList.length > 0 ? (
            subjectList.map(item => (
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
            ))
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
