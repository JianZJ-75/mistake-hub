import { useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Image, Input } from '@tarojs/components'
import { currentDetail, modifyDailyLimit, modifyProfile } from '../../service/account'
import { uploadImage } from '../../service/upload'
import { statsOverview, statsMastery, statsStreak, statsDailyCompletion, statsMasteryTrend } from '../../service/stats'
import { AccountDetailResp, StatsOverviewResp, MasteryDistributionResp, StreakResp, DailyCompletionResp, MasteryTrendResp } from '../../types'
import './index.scss'

/** 格式化日期为 M/D */
const formatMD = (dateStr: string): string => {

  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

/** 完成率柱状颜色三段 */
const getCompletionColor = (rate: number | null): string => {

  if (rate === null || rate < 0.5) return '#F44336'
  if (rate < 0.8) return '#FF9800'
  return '#4CAF50'
}

/** 掌握度散点颜色三段 */
const getMasteryDotColor = (avg: number): string => {

  if (avg < 60) return '#F44336'
  if (avg < 80) return '#FF9800'
  return '#4CAF50'
}

const ProfilePage = () => {

  // ===== 用户信息 =====
  const [account, setAccount] = useState<AccountDetailResp | null>(null)

  // ===== 学习数据 =====
  const [overview, setOverview] = useState<StatsOverviewResp | null>(null)
  const [mastery, setMastery] = useState<MasteryDistributionResp | null>(null)
  const [streak, setStreak] = useState<StreakResp | null>(null)
  const [dailyCompletion, setDailyCompletion] = useState<DailyCompletionResp[]>([])
  const [masteryTrend, setMasteryTrend] = useState<MasteryTrendResp[]>([])

  // ===== 每日复习量设置 =====
  const [dailyLimit, setDailyLimit] = useState('')
  const [saving, setSaving] = useState(false)

  // ===== 编辑昵称弹窗 =====
  const [nicknameModalOpen, setNicknameModalOpen] = useState(false)
  const [editNickname, setEditNickname] = useState('')
  const [savingNickname, setSavingNickname] = useState(false)

  // ===== 头像上传 =====
  const [uploadingAvatar, setUploadingAvatar] = useState(false)

  useDidShow(() => {
    loadData()
  })

  const loadData = () => {

    currentDetail()
      .then(data => {
        setAccount(data)
        setDailyLimit(String(data.dailyLimit))
      })
      .catch(() => {})

    statsOverview().then(setOverview).catch(() => {})
    statsMastery().then(setMastery).catch(() => {})
    statsStreak().then(setStreak).catch(() => {})
    statsDailyCompletion().then(data => setDailyCompletion(data || [])).catch(() => {})
    statsMasteryTrend().then(data => setMasteryTrend(data || [])).catch(() => {})
  }

  const handleSaveDailyLimit = async () => {

    const val = Number(dailyLimit)
    if (!val || val < 1 || val > 200) {
      Taro.showToast({ title: '请输入 1~200 的整数', icon: 'none' })
      return
    }
    setSaving(true)
    try {
      await modifyDailyLimit(val)
      Taro.showToast({ title: '保存成功', icon: 'success' })
    } catch {
      // 错误已 toast
    } finally {
      setSaving(false)
    }
  }

  const handleAvatarTap = async () => {

    if (uploadingAvatar) return
    try {
      const res = await Taro.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'] })
      const filePath = res.tempFiles[0].tempFilePath
      setUploadingAvatar(true)
      Taro.showLoading({ title: '上传中...' })
      const url = await uploadImage(filePath)
      await modifyProfile({ avatarUrl: url })
      setAccount(prev => prev ? { ...prev, avatarUrl: url } : prev)
      Taro.showToast({ title: '头像已更新', icon: 'success' })
    } catch {
      // 错误已 toast
    } finally {
      setUploadingAvatar(false)
      Taro.hideLoading()
    }
  }

  const handleOpenNicknameModal = () => {

    setEditNickname(account?.nickname || '')
    setNicknameModalOpen(true)
  }

  const handleSaveNickname = async () => {

    const val = editNickname.trim()
    if (!val) {
      Taro.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    if (val === account?.nickname) {
      setNicknameModalOpen(false)
      return
    }
    setSavingNickname(true)
    try {
      await modifyProfile({ nickname: val })
      setAccount(prev => prev ? { ...prev, nickname: val } : prev)
      Taro.showToast({ title: '昵称已更新', icon: 'success' })
      setNicknameModalOpen(false)
    } catch {
      // 错误已 toast
    } finally {
      setSavingNickname(false)
    }
  }

  const masteryTotal = mastery ? (mastery.notMastered + mastery.learning + mastery.mastered) : 0
  const redPct = masteryTotal > 0 ? (mastery!.notMastered / masteryTotal * 100) : 0
  const orangePct = masteryTotal > 0 ? (mastery!.learning / masteryTotal * 100) : 0
  const greenPct = masteryTotal > 0 ? (mastery!.mastered / masteryTotal * 100) : 0

  return (
    <View className='profile-page'>
      {/* 用户信息卡片 */}
      <View className='user-card'>
        <View className='user-avatar-wrap' onClick={handleAvatarTap}>
          <View className='user-avatar'>
            {account?.avatarUrl ? (
              <Image className='user-avatar-img' src={account.avatarUrl} mode='aspectFill' />
            ) : (
              <View className='avatar-placeholder'>
                <Text>{account?.nickname?.charAt(0) || '?'}</Text>
              </View>
            )}
          </View>
          <View className='avatar-edit-badge'>
            <Text className='avatar-edit-icon'>✎</Text>
          </View>
          {uploadingAvatar && <View className='avatar-loading'><Text className='avatar-loading-text'>...</Text></View>}
        </View>
        <View className='user-info'>
          <View className='user-nickname-row' onClick={handleOpenNicknameModal}>
            <Text className='user-nickname'>{account?.nickname || '加载中...'}</Text>
            <Text className='nickname-edit-icon'>✎</Text>
          </View>
          <Text className='user-role'>{account?.role === 'admin' ? '管理员' : '学生'}</Text>
        </View>
      </View>

      {/* 昵称编辑弹窗 */}
      {nicknameModalOpen && (
        <View className='modal-overlay' onClick={() => setNicknameModalOpen(false)}>
          <View className='modal-content' onClick={e => e.stopPropagation()}>
            <Text className='modal-title'>修改昵称</Text>
            <Input
              className='modal-input'
              value={editNickname}
              onInput={e => setEditNickname(e.detail.value)}
              placeholder='请输入昵称'
              maxlength={20}
              focus
            />
            <View className='modal-footer'>
              <View className='modal-btn modal-btn-cancel' onClick={() => setNicknameModalOpen(false)}>
                <Text className='modal-btn-text'>取消</Text>
              </View>
              <View
                className={`modal-btn modal-btn-confirm ${savingNickname ? 'modal-btn-disabled' : ''}`}
                onClick={savingNickname ? undefined : handleSaveNickname}
              >
                <Text className='modal-btn-text-confirm'>{savingNickname ? '保存中...' : '确认'}</Text>
              </View>
            </View>
          </View>
        </View>
      )}

      {/* 学习数据概览 */}
      <Text className='section-title'>学习概览</Text>
      <View className='stats-row'>
        <View className='stats-item'>
          <Text className='stats-item-value color-blue'>{overview?.totalMistakes ?? '—'}</Text>
          <Text className='stats-item-label'>总错题</Text>
        </View>
        <View className='stats-item'>
          <Text className='stats-item-value color-green'>{overview?.masteredCount ?? '—'}</Text>
          <Text className='stats-item-label'>已掌握</Text>
        </View>
      </View>
      <View className='stats-row'>
        <View className='stats-item'>
          <Text className='stats-item-value color-orange'>{overview?.pendingReviewCount ?? '—'}</Text>
          <Text className='stats-item-label'>待复习</Text>
        </View>
        <View className='stats-item'>
          <Text className='stats-item-value color-purple'>{overview?.newThisWeek ?? '—'}</Text>
          <Text className='stats-item-label'>本周新增</Text>
        </View>
      </View>

      {/* 连续天数 */}
      <View className='streak-row'>
        <View className='streak-card'>
          <Text className='streak-value'>{streak?.currentStreak ?? '—'}</Text>
          <Text className='streak-label'>连续复习天数</Text>
        </View>
        <View className='streak-card'>
          <Text className='streak-value'>{streak?.longestStreak ?? '—'}</Text>
          <Text className='streak-label'>最长连续天数</Text>
        </View>
      </View>

      {/* 掌握度分布 */}
      <Text className='section-title'>掌握度分布</Text>
      <View className='mastery-card'>
        {masteryTotal > 0 ? (
          <>
            <View className='mastery-bar-bg'>
              <View className='mastery-bar-red' style={{ width: `${redPct}%` }} />
              <View className='mastery-bar-orange' style={{ width: `${orangePct}%` }} />
              <View className='mastery-bar-green' style={{ width: `${greenPct}%` }} />
            </View>
            <View className='mastery-legend'>
              <View className='mastery-legend-item'>
                <View className='mastery-legend-dot dot-red' />
                <Text className='mastery-legend-text'>未掌握</Text>
                <Text className='mastery-legend-num'>{mastery!.notMastered}</Text>
              </View>
              <View className='mastery-legend-item'>
                <View className='mastery-legend-dot dot-orange' />
                <Text className='mastery-legend-text'>掌握中</Text>
                <Text className='mastery-legend-num'>{mastery!.learning}</Text>
              </View>
              <View className='mastery-legend-item'>
                <View className='mastery-legend-dot dot-green' />
                <Text className='mastery-legend-text'>已掌握</Text>
                <Text className='mastery-legend-num'>{mastery!.mastered}</Text>
              </View>
            </View>
          </>
        ) : (
          <Text className='loading-text'>暂无数据</Text>
        )}
      </View>

      {/* 学习趋势 */}
      <Text className='section-title'>学习趋势</Text>

      {/* 每日完成率柱状图 */}
      <View className='chart-card'>
        <Text className='chart-subtitle'>每日复习完成率（近 30 天）</Text>
        {dailyCompletion.length > 0 ? (
          <View className='chart-wrap'>
            <View className='chart-y-axis'>
              <Text className='chart-y-label'>100%</Text>
              <Text className='chart-y-label'>50%</Text>
              <Text className='chart-y-label'>0%</Text>
            </View>
            <View className='chart-body'>
              <View className='chart-ref-line chart-ref-top' />
              <View className='chart-ref-line chart-ref-mid' />
              <View className='chart-bars'>
                {dailyCompletion.map((item, i) => (
                  <View className='chart-bar-col' key={i}>
                    <View
                      className='chart-bar'
                      style={{
                        height: `${(item.completionRate ?? 0) * 100}%`,
                        background: getCompletionColor(item.completionRate)
                      }}
                    />
                  </View>
                ))}
              </View>
              <View className='chart-x-axis'>
                <Text className='chart-x-label'>{formatMD(dailyCompletion[0].date)}</Text>
                <Text className='chart-x-label'>{formatMD(dailyCompletion[Math.floor(dailyCompletion.length / 2)].date)}</Text>
                <Text className='chart-x-label'>{formatMD(dailyCompletion[dailyCompletion.length - 1].date)}</Text>
              </View>
            </View>
          </View>
        ) : (
          <Text className='loading-text'>暂无数据</Text>
        )}
      </View>

      {/* 掌握度趋势散点图 */}
      <View className='chart-card'>
        <Text className='chart-subtitle'>掌握度趋势（近 30 天）</Text>
        {masteryTrend.length > 0 ? (
          <View className='chart-wrap'>
            <View className='chart-y-axis'>
              <Text className='chart-y-label'>100</Text>
              <Text className='chart-y-label'>50</Text>
              <Text className='chart-y-label'>0</Text>
            </View>
            <View className='chart-body'>
              <View className='chart-ref-line chart-ref-top' />
              <View className='chart-ref-line chart-ref-mid' />
              <View className='chart-dots'>
                {masteryTrend.map((item, i) => (
                  <View className='chart-dot-col' key={i}>
                    <View
                      className='chart-dot'
                      style={{
                        bottom: `${item.avgMastery}%`,
                        background: getMasteryDotColor(item.avgMastery)
                      }}
                    />
                  </View>
                ))}
              </View>
              <View className='chart-x-axis'>
                <Text className='chart-x-label'>{formatMD(masteryTrend[0].date)}</Text>
                <Text className='chart-x-label'>{formatMD(masteryTrend[Math.floor(masteryTrend.length / 2)].date)}</Text>
                <Text className='chart-x-label'>{formatMD(masteryTrend[masteryTrend.length - 1].date)}</Text>
              </View>
            </View>
          </View>
        ) : (
          <Text className='loading-text'>暂无数据</Text>
        )}
      </View>

      {/* 每日复习量设置 */}
      <Text className='section-title'>复习设置</Text>
      <View className='setting-card'>
        <View className='setting-row'>
          <Text className='setting-label'>每日复习量</Text>
          <Input
            className='setting-input'
            type='number'
            value={dailyLimit}
            onInput={e => setDailyLimit(e.detail.value)}
            placeholder='30'
          />
          <View
            className={`setting-btn ${saving ? 'setting-btn-disabled' : ''}`}
            onClick={saving ? undefined : handleSaveDailyLimit}
          >
            <Text className='setting-btn-text'>{saving ? '保存中...' : '保存'}</Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default ProfilePage
