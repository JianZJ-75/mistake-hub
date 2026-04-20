import { useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import { reviewProgress } from '../../service/review'
import { statsSubject } from '../../service/stats'
import { ReviewProgressResp, SubjectStatsResp } from '../../types'
import { COLOR_PRIMARY, COLOR_TRACK, COLOR_MASTERY_HIGH, COLOR_MASTERY_MID, COLOR_MASTERY_LOW, getBarColor } from '../../utils/colors'
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
