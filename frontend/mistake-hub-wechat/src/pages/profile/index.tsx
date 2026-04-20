import { useState, useMemo, useRef, useEffect, useCallback } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Image, Input } from '@tarojs/components'
import { currentDetail, modifyDailyLimit, modifyProfile } from '../../service/account'
import { uploadImage } from '../../service/upload'
import { statsOverview, statsMastery, statsStreak, statsDailyCompletion, statsMasteryTrend } from '../../service/stats'
import { AccountDetailResp, StatsOverviewResp, MasteryDistributionResp, StreakResp, DailyCompletionResp, MasteryTrendResp } from '../../types'
import { getCompletionColor, getMasteryDotColor, COLOR_PRIMARY, COLOR_MASTERY_HIGH, COLOR_MASTERY_LOW } from '../../utils/colors'
import { buildLineChartSvg, svgToBgUrl } from '../../utils/charts'
import './index.scss'

/** 格式化日期为 M/D */
const formatMD = (dateStr: string): string => {

  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

/** 以今天为 end 计算 start / mid / end 三个 M/D 字符串（用于 X 轴标签），独立于数据稀疏度 */
const rangeLabels = (days: number): { start: string; mid: string; end: string } => {

  const now = new Date()
  const mkLabel = (offset: number) => {
    const d = new Date(now)
    d.setDate(d.getDate() - offset)
    return `${d.getMonth() + 1}/${d.getDate()}`
  }
  return {
    start: mkLabel(days - 1),
    mid: mkLabel(Math.floor((days - 1) / 2)),
    end: mkLabel(0),
  }
}

const DAYS_OPTIONS: Array<7 | 30 | 90> = [7, 30, 90]

const ProfilePage = () => {

  // ===== 用户信息 =====
  const [account, setAccount] = useState<AccountDetailResp | null>(null)

  // ===== 学习数据 =====
  const [overview, setOverview] = useState<StatsOverviewResp | null>(null)
  const [mastery, setMastery] = useState<MasteryDistributionResp | null>(null)
  const [streak, setStreak] = useState<StreakResp | null>(null)
  const [dailyCompletion, setDailyCompletion] = useState<DailyCompletionResp[]>([])
  const [masteryTrend, setMasteryTrend] = useState<MasteryTrendResp[]>([])

  // ===== 学习趋势交互 =====
  const [daysRange, setDaysRange] = useState<7 | 30 | 90>(30)
  const [selectedCompletionIdx, setSelectedCompletionIdx] = useState<number | null>(null)
  const [selectedMasteryIdx, setSelectedMasteryIdx] = useState<number | null>(null)

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
    loadBasicData()
    loadTrends(daysRange)
  })

  const loadBasicData = () => {

    currentDetail()
      .then(data => {
        setAccount(data)
        setDailyLimit(String(data.dailyLimit))
      })
      .catch(() => {})

    statsOverview().then(setOverview).catch(() => {})
    statsMastery().then(setMastery).catch(() => {})
    statsStreak().then(setStreak).catch(() => {})
  }

  const loadTrends = (days: number) => {

    statsDailyCompletion(days).then(data => setDailyCompletion(data || [])).catch(() => {})
    statsMasteryTrend(days).then(data => setMasteryTrend(data || [])).catch(() => {})
  }

  const handleDaysChange = (days: 7 | 30 | 90) => {

    if (days === daysRange) return
    setDaysRange(days)
    setSelectedCompletionIdx(null)
    setSelectedMasteryIdx(null)
    loadTrends(days)
  }

  // ===== 摘要数字 =====

  const avgCompletionRate = useMemo(() => {
    const valid = dailyCompletion.filter(d => d.completionRate !== null)
    if (valid.length === 0) return null
    const mean = valid.reduce((s, d) => s + (d.completionRate || 0), 0) / valid.length
    return Math.round(mean * 100)
  }, [dailyCompletion])

  const masteryDelta = useMemo(() => {
    const valid = masteryTrend.filter(d => d.avgMastery !== null)
    if (valid.length < 2) return null
    const first = valid[0].avgMastery as number
    const last = valid[valid.length - 1].avgMastery as number
    return Math.round(last - first)
  }, [masteryTrend])

  // 折线图用 SVG 作背景贴图，纯 View 渲染，不走原生 Canvas
  const masterySvgBg = useMemo(() => {
    if (masteryTrend.length === 0) return 'none'
    const svg = buildLineChartSvg({
      data: masteryTrend.map(d => ({ value: d.avgMastery })),
      lineColor: COLOR_PRIMARY,
    })
    return svgToBgUrl(svg)
  }, [masteryTrend])

  // style 对象 memo 住，避免每次 render 生成新引用导致 Taro 重推 data URL 给 weapp
  const lineChartStyle = useMemo(() => ({ backgroundImage: masterySvgBg }), [masterySvgBg])

  const toggleCompletion = (i: number) => setSelectedCompletionIdx(prev => (prev === i ? null : i))
  const toggleMastery = (i: number) => setSelectedMasteryIdx(prev => (prev === i ? null : i))

  // ===== 折线图触摸命中 =====

  const LINE_HIT_ID = 'line-hit-overlay'
  const lineHitBounds = useRef<{ left: number; width: number } | null>(null)

  useEffect(() => {
    if (masteryTrend.length === 0) return
    setTimeout(() => {
      Taro.createSelectorQuery()
        .select(`#${LINE_HIT_ID}`)
        .boundingClientRect(res => {
          const rect = Array.isArray(res) ? res[0] : res
          if (rect) lineHitBounds.current = { left: rect.left as number, width: rect.width as number }
        })
        .exec()
    }, 100)
  }, [masteryTrend])

  const handleLineTap = useCallback((e) => {

    const bounds = lineHitBounds.current
    if (!bounds || bounds.width === 0 || masteryTrend.length === 0) return
    const clientX = e.touches?.[0]?.clientX
    if (clientX == null) return
    const idx = Math.round((clientX - bounds.left) / bounds.width * (masteryTrend.length - 1))
    if (idx >= 0 && idx < masteryTrend.length) toggleMastery(idx)
  }, [masteryTrend.length])

  // ===== 其它交互 =====

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

  const masteryTotal = mastery ? (mastery.stranger + mastery.beginner + mastery.basic + mastery.proficient + mastery.mastered) : 0
  const redPct = masteryTotal > 0 ? ((mastery!.stranger + mastery!.beginner) / masteryTotal * 100) : 0
  const orangePct = masteryTotal > 0 ? ((mastery!.basic + mastery!.proficient) / masteryTotal * 100) : 0
  const greenPct = masteryTotal > 0 ? (mastery!.mastered / masteryTotal * 100) : 0

  const selectedCompletion = selectedCompletionIdx !== null ? dailyCompletion[selectedCompletionIdx] : null
  const selectedMastery = selectedMasteryIdx !== null ? masteryTrend[selectedMasteryIdx] : null

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
                <Text className='mastery-legend-num'>{mastery!.stranger + mastery!.beginner}</Text>
              </View>
              <View className='mastery-legend-item'>
                <View className='mastery-legend-dot dot-orange' />
                <Text className='mastery-legend-text'>进步中</Text>
                <Text className='mastery-legend-num'>{mastery!.basic + mastery!.proficient}</Text>
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
      <View className='trend-header'>
        <Text className='section-title trend-section-title'>学习趋势</Text>
        <View className='trend-tabs'>
          {DAYS_OPTIONS.map(d => (
            <View
              key={d}
              className={`trend-tab ${daysRange === d ? 'trend-tab-active' : ''}`}
              onClick={() => handleDaysChange(d)}
            >
              <Text className='trend-tab-text'>{d}天</Text>
            </View>
          ))}
        </View>
      </View>

      {/* 每日完成率柱状图 */}
      <View className='chart-card'>
        <View className='trend-card-head'>
          <Text className='trend-summary-value' style={{ color: COLOR_PRIMARY }}>
            {avgCompletionRate !== null ? `${avgCompletionRate}%` : '—'}
          </Text>
          <Text className='trend-summary-label'>近 {daysRange} 天平均完成率</Text>
        </View>
        <Text className='chart-subtitle'>每日复习完成率 · 点击柱子查看详情</Text>
        {dailyCompletion.length > 0 ? (
          <>
            <View className='chart-wrap'>
              <View className='chart-y-axis'>
                <Text className='chart-y-label'>100%</Text>
                <Text className='chart-y-label'>50%</Text>
                <Text className='chart-y-label'>0%</Text>
              </View>
              <View className='chart-body-wrap'>
                <View className='chart-plot'>
                  <View className='chart-ref-line chart-ref-top' />
                  <View className='chart-ref-line chart-ref-mid' />
                  <View className='chart-ref-line chart-ref-bottom' />
                  <View className='bar-chart'>
                    {dailyCompletion.map((d, i) => {
                      const v = d.completionRate
                      const showBar = v != null && v > 0
                      const h = showBar ? Math.min(1, v!) * 100 : 0
                      const selected = selectedCompletionIdx === i
                      return (
                        <View key={i} className='bar-col'>
                          {showBar && (
                            <View
                              className={`bar-fill ${selected ? 'bar-fill-selected' : ''}`}
                              style={{ height: `${h}%`, background: getCompletionColor(v) }}
                            />
                          )}
                        </View>
                      )
                    })}
                  </View>
                </View>
                <View className='chart-hit-layer'>
                  {dailyCompletion.map((_, i) => (
                    <View key={i} className='chart-hit-col' onClick={() => toggleCompletion(i)} />
                  ))}
                </View>
                <View className='chart-x-axis'>
                  <Text className='chart-x-label'>{rangeLabels(daysRange).start}</Text>
                  <Text className='chart-x-label'>{rangeLabels(daysRange).mid}</Text>
                  <Text className='chart-x-label'>{rangeLabels(daysRange).end}</Text>
                </View>
              </View>
            </View>
            <View className='trend-detail'>
              {selectedCompletion ? (
                <Text className='trend-detail-text'>
                  {formatMD(selectedCompletion.date)} · 完成率 {Math.round((selectedCompletion.completionRate ?? 0) * 100)}% ({selectedCompletion.completed}/{selectedCompletion.totalPlanned})
                </Text>
              ) : (
                <Text className='trend-detail-placeholder'>点击柱子查看当日完成情况</Text>
              )}
            </View>
            <View className='trend-legend'>
              <View className='trend-legend-item'>
                <View className='trend-legend-dot' style={{ background: COLOR_MASTERY_HIGH }} />
                <Text className='trend-legend-text'>达标 ≥80%</Text>
              </View>
              <View className='trend-legend-item'>
                <View className='trend-legend-dot' style={{ background: '#F59E0B' }} />
                <Text className='trend-legend-text'>一般 50-80%</Text>
              </View>
              <View className='trend-legend-item'>
                <View className='trend-legend-dot' style={{ background: COLOR_MASTERY_LOW }} />
                <Text className='trend-legend-text'>偏低 {'<'}50%</Text>
              </View>
            </View>
          </>
        ) : (
          <Text className='loading-text'>暂无数据</Text>
        )}
      </View>

      {/* 掌握度趋势折线图 */}
      <View className='chart-card'>
        <View className='trend-card-head'>
          <Text
            className='trend-summary-value'
            style={{ color: masteryDelta === null ? '#94A3B8' : masteryDelta >= 0 ? COLOR_MASTERY_HIGH : COLOR_MASTERY_LOW }}
          >
            {masteryDelta === null ? '—' : `${masteryDelta >= 0 ? '+' : ''}${masteryDelta}`}
          </Text>
          <Text className='trend-summary-label'>近 {daysRange} 天掌握度变化</Text>
        </View>
        <Text className='chart-subtitle'>复习后平均掌握度（0-100） · 点击节点查看详情</Text>
        {masteryTrend.length > 0 ? (
          <>
            <View className='chart-wrap'>
              <View className='chart-y-axis'>
                <Text className='chart-y-label'>100</Text>
                <Text className='chart-y-label'>50</Text>
                <Text className='chart-y-label'>0</Text>
              </View>
              <View className='chart-body-wrap'>
                <View className='chart-plot'>
                  <View className='chart-ref-line chart-ref-top' />
                  <View className='chart-ref-line chart-ref-mid' />
                  <View className='chart-ref-line chart-ref-bottom' />
                  <View className='line-chart' style={lineChartStyle} />
                  {masteryTrend.map((d, i) => {
                    if (d.avgMastery == null) return null
                    const leftPct = masteryTrend.length > 1 ? (i / (masteryTrend.length - 1)) * 100 : 50
                    const topPct = 100 - Math.max(0, Math.min(100, d.avgMastery))
                    const selected = selectedMasteryIdx === i
                    return (
                      <View
                        key={i}
                        className={`line-dot ${selected ? 'line-dot-selected' : ''}`}
                        style={{
                          left: `${leftPct}%`,
                          top: `${topPct}%`,
                          background: getMasteryDotColor(d.avgMastery),
                        }}
                      />
                    )
                  })}
                </View>
                <View id={LINE_HIT_ID} className='chart-hit-overlay' onTouchStart={handleLineTap} />
                <View className='chart-x-axis'>
                  <Text className='chart-x-label'>{rangeLabels(daysRange).start}</Text>
                  <Text className='chart-x-label'>{rangeLabels(daysRange).mid}</Text>
                  <Text className='chart-x-label'>{rangeLabels(daysRange).end}</Text>
                </View>
              </View>
            </View>
            <View className='trend-detail'>
              {selectedMastery ? (
                <Text className='trend-detail-text'>
                  {selectedMastery.avgMastery !== null
                    ? `${formatMD(selectedMastery.date)} · 平均掌握度 ${selectedMastery.avgMastery}`
                    : `${formatMD(selectedMastery.date)} · 当日无复习记录`}
                </Text>
              ) : (
                <Text className='trend-detail-placeholder'>点击节点查看当日掌握度</Text>
              )}
            </View>
          </>
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
