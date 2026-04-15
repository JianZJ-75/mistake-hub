import { useState, useRef, useEffect, useCallback } from 'react'
import Taro, { useLoad, useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import { View, Text, ScrollView } from '@tarojs/components'
import { mistakeList } from '../../service/mistake'
import { tagTree, tagCustomList } from '../../service/tag'
import { statsMastery } from '../../service/stats'
import { MistakeDetailResp, TagResp, MasteryDistributionResp } from '../../types'
import { getMasteryInfo, getFormattedId, collectTagIds } from '../../utils/mistake'
import TagPickerModal from '../../components/TagPickerModal'
import './index.scss'

const PAGE_SIZE = 10

const MistakePage = () => {

  // ===== 列表状态 =====
  const [mistakes, setMistakes] = useState<MistakeDetailResp[]>([])
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)

  // ===== 统计状态 =====
  const [stats, setStats] = useState<MasteryDistributionResp | null>(null)

  // ===== 筛选状态 =====
  const [masteryFilter, setMasteryFilter] = useState<number | undefined>(undefined)
  const [activeSubjectId, setActiveSubjectId] = useState<number | undefined>(undefined)
  const [globalTags, setGlobalTags] = useState<TagResp[]>([])
  const [customTags, setCustomTags] = useState<TagResp[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])
  const [showTagPicker, setShowTagPicker] = useState(false)

  // ===== 防双重请求 ref =====
  const loadingRef = useRef(false)
  const pageNumRef = useRef(1)
  const filterRef = useRef<{ mastery?: number; tagIds?: string }>({})

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleMistakeChanged = useCallback(() => {
    fetchList(1, true)
    loadStats()
  }, [])

  useEffect(() => {
    tagTree().then(tree => setGlobalTags(tree)).catch(() => {})
    loadCustomTags()
    loadStats()
    Taro.eventCenter.on('mistakeChanged', handleMistakeChanged)
    return () => { Taro.eventCenter.off('mistakeChanged', handleMistakeChanged) }
  }, [handleMistakeChanged])

  const loadCustomTags = () => {

    tagCustomList().then(list => setCustomTags(list)).catch(() => {})
  }

  const loadStats = () => {

    statsMastery().then(data => setStats(data)).catch(() => {})
  }

  const fetchList = async (page: number, reset: boolean) => {

    if (loadingRef.current) return
    loadingRef.current = true
    setLoading(true)
    try {
      const f = filterRef.current
      const res = await mistakeList({
        masteryFilter: f.mastery,
        tagIds: f.tagIds,
        pageNum: page,
        pageSize: PAGE_SIZE,
      })
      const records = res.records || []
      setMistakes(prev => reset ? records : [...prev, ...records])
      setHasMore(records.length === PAGE_SIZE)
      pageNumRef.current = page
    } catch {
      // request.ts 已处理 toast
    } finally {
      loadingRef.current = false
      setLoading(false)
      if (reset) Taro.stopPullDownRefresh()
    }
  }

  const isFirstShow = useRef(true)

  useLoad(() => {
    fetchList(1, true)
  })

  useDidShow(() => {
    if (isFirstShow.current) {
      isFirstShow.current = false
      return
    }
    tagTree().then(tree => setGlobalTags(tree)).catch(() => {})
    loadCustomTags()
    loadStats()
    fetchList(1, true)
  })

  usePullDownRefresh(() => {
    loadStats()
    fetchList(1, true)
  })

  useReachBottom(() => {
    if (hasMore && !loadingRef.current) {
      fetchList(pageNumRef.current + 1, false)
    }
  })

  /** 点击统计头部的掌握度筛选 */
  const handleStatClick = (mastery?: number) => {

    const newMastery = masteryFilter === mastery ? undefined : mastery
    setMasteryFilter(newMastery)
    filterRef.current = { ...filterRef.current, mastery: newMastery }
    fetchList(1, true)
  }

  /** 点击学科 chip */
  const handleSubjectClick = (tag?: TagResp) => {

    if (!tag) {
      // "全部学科"
      setActiveSubjectId(undefined)
      setSelectedTagIds([])
      filterRef.current = { ...filterRef.current, tagIds: undefined }
    } else {
      setActiveSubjectId(tag.id)
      const ids = collectTagIds(tag)
      setSelectedTagIds(ids)
      filterRef.current = { ...filterRef.current, tagIds: ids.join(',') }
    }
    fetchList(1, true)
  }

  /** 标签弹窗确认后应用筛选 */
  const handleTagConfirm = (ids: number[]) => {

    setSelectedTagIds(ids)
    setActiveSubjectId(undefined)
    const tagIdsStr = ids.length > 0 ? ids.join(',') : undefined
    filterRef.current = { ...filterRef.current, tagIds: tagIdsStr }
    fetchList(1, true)
  }

  // ===== 派生数据 =====
  const subjectTags = globalTags.filter(t => t.type === 'SUBJECT')
  const totalCount = stats ? stats.notMastered + stats.learning + stats.mastered : 0
  const learningCount = stats ? stats.learning + stats.mastered : 0

  return (
    <View className='list-page'>
      {/* 统计头部 */}
      <View className='stats-header'>
        <View
          className={`stat-item ${masteryFilter === undefined ? 'stat-active' : ''}`}
          onClick={() => handleStatClick(undefined)}
        >
          <Text className='stat-label'>全部题目</Text>
          <Text className='stat-num stat-num-total'>{totalCount}</Text>
        </View>
        <View
          className={`stat-item ${masteryFilter === 0 ? 'stat-active' : ''}`}
          onClick={() => handleStatClick(0)}
        >
          <Text className='stat-label'>未掌握</Text>
          <Text className='stat-num stat-num-danger'>{stats?.notMastered ?? 0}</Text>
        </View>
        <View
          className={`stat-item ${masteryFilter === 1 ? 'stat-active' : ''}`}
          onClick={() => handleStatClick(1)}
        >
          <Text className='stat-label'>掌握中</Text>
          <Text className='stat-num stat-num-purple'>{learningCount}</Text>
        </View>
      </View>

      {/* 学科筛选栏 */}
      <ScrollView className='subject-bar' scrollX enableFlex>
        <View className='subject-chips'>
          <View
            className={`subject-chip ${activeSubjectId === undefined && selectedTagIds.length === 0 ? 'chip-active' : ''}`}
            onClick={() => handleSubjectClick()}
          >
            <Text>全部学科</Text>
          </View>
          {subjectTags.map(tag => (
            <View
              key={tag.id}
              className={`subject-chip ${activeSubjectId === tag.id ? 'chip-active' : ''}`}
              onClick={() => handleSubjectClick(tag)}
            >
              <Text>{tag.name}</Text>
            </View>
          ))}
          <View
            className={`subject-chip chip-filter ${activeSubjectId === undefined && selectedTagIds.length > 0 ? 'chip-active' : ''}`}
            onClick={() => setShowTagPicker(true)}
          >
            <Text>筛选</Text>
          </View>
        </View>
      </ScrollView>

      {/* 错题列表 */}
      <View className='mistake-list'>
        {mistakes.map(m => {
          const mastery = getMasteryInfo(m.masteryLevel || 0)
          const fmtId = getFormattedId(m.id, m.tags)
          return (
            <View
              key={m.id}
              className='mistake-card'
              onClick={() => Taro.navigateTo({ url: `/pages/mistake-detail/index?id=${m.id}` })}
            >
              <View className='card-top'>
                <Text className={`mastery-badge ${mastery.cls}`}>{mastery.label}</Text>
                <Text className='card-id'>ID: {fmtId}</Text>
              </View>
              <Text className='card-title'>{m.title}</Text>
              <View className='card-bottom'>
                <View className='depth-info'>
                  <Text className='depth-label'>掌握深度：{mastery.depthText}</Text>
                  <View className='depth-bar'>
                    {[1, 2, 3, 4, 5].map(i => (
                      <View key={i} className={`depth-seg ${i <= mastery.depthLevel ? 'seg-filled' : 'seg-empty'}`} />
                    ))}
                  </View>
                </View>
                <View className='card-action-btn'>
                  <Text className='card-action-text'>{mastery.actionText} &gt;</Text>
                </View>
              </View>
            </View>
          )
        })}

        {!loading && mistakes.length === 0 && (
          <View className='empty-wrap'>
            <Text className='empty-icon'>📋</Text>
            <Text className='empty-text'>暂无错题</Text>
            <Text className='empty-hint'>点击右下角 + 开始录入</Text>
          </View>
        )}

        {loading && (
          <View className='footer-row'>
            <Text className='footer-text'>加载中…</Text>
          </View>
        )}

        {!hasMore && mistakes.length > 0 && (
          <View className='footer-row'>
            <Text className='footer-text'>已加载全部</Text>
          </View>
        )}
      </View>

      {/* 悬浮录入按钮 */}
      <View className='fab' onClick={() => Taro.navigateTo({ url: '/pages/mistake-add/index' })}>
        <Text className='fab-text'>+</Text>
      </View>

      {/* 标签多选弹窗 */}
      <TagPickerModal
        visible={showTagPicker}
        title='选择筛选标签'
        tags={globalTags}
        customTags={customTags}
        mode='multi'
        autoExpand
        selectedIds={selectedTagIds}
        onConfirm={handleTagConfirm}
        onClose={() => setShowTagPicker(false)}
        onCustomTagsChange={loadCustomTags}
      />
    </View>
  )
}

export default MistakePage
