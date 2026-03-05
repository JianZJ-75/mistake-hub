import { useState, useRef, useEffect, useCallback } from 'react'
import Taro, { useLoad, useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import { View, Text, Image, Input, ScrollView } from '@tarojs/components'
import { mistakeList } from '../../service/mistake'
import { tagTree } from '../../service/tag'
import { MistakeDetailResp, TagResp } from '../../types'
import { flattenTags, getMasteryInfo } from '../../utils/mistake'
import './index.scss'

const PAGE_SIZE = 10

const MASTERY_FILTERS = [
  { label: '全部', value: undefined as number | undefined },
  { label: '未掌握', value: 0 },
  { label: '掌握中', value: 1 },
  { label: '已掌握', value: 2 },
]

const MistakePage = () => {
  // ===== 列表状态 =====
  const [mistakes, setMistakes] = useState<MistakeDetailResp[]>([])
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)

  // ===== 筛选状态 =====
  const [activeFilter, setActiveFilter] = useState(0)
  const [subjectInput, setSubjectInput] = useState('')
  const [selectedTag, setSelectedTag] = useState<TagResp | null>(null)
  const [showTagFilter, setShowTagFilter] = useState(false)
  const [allTags, setAllTags] = useState<TagResp[]>([])

  // ===== 防双重请求 ref =====
  const loadingRef = useRef(false)
  const pageNumRef = useRef(1)
  const filterRef = useRef<{ mastery?: number; subject?: string; tagId?: number }>({})

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleMistakeChanged = useCallback(() => fetchList(1, true), [])

  useEffect(() => {
    tagTree().then(tree => setAllTags(flattenTags(tree))).catch(() => {})
    // 监听错题变更事件（新增/编辑/删除后刷新列表）
    Taro.eventCenter.on('mistakeChanged', handleMistakeChanged)
    return () => { Taro.eventCenter.off('mistakeChanged', handleMistakeChanged) }
  }, [handleMistakeChanged])

  const fetchList = async (page: number, reset: boolean) => {
    if (loadingRef.current) return
    loadingRef.current = true
    setLoading(true)
    try {
      const f = filterRef.current
      const res = await mistakeList({
        masteryFilter: f.mastery,
        subject: f.subject || undefined,
        tagId: f.tagId,
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
    fetchList(1, true)
  })

  usePullDownRefresh(() => {
    fetchList(1, true)
  })

  useReachBottom(() => {
    if (hasMore && !loadingRef.current) {
      fetchList(pageNumRef.current + 1, false)
    }
  })

  const applyFilter = (mastery?: number, subject?: string, tagId?: number) => {
    filterRef.current = { mastery, subject, tagId }
    fetchList(1, true)
  }

  const handleMasteryChange = (index: number, value?: number) => {
    setActiveFilter(index)
    applyFilter(value, filterRef.current.subject, filterRef.current.tagId)
  }

  const handleSubjectSearch = () => {
    applyFilter(filterRef.current.mastery, subjectInput.trim() || undefined, filterRef.current.tagId)
  }

  const handleSelectTag = (tag: TagResp | null) => {
    setSelectedTag(tag)
    setShowTagFilter(false)
    applyFilter(filterRef.current.mastery, filterRef.current.subject, tag?.id)
  }

  return (
    <View className='list-page'>
      {/* 掌握度筛选条 */}
      <View className='filter-bar'>
        {MASTERY_FILTERS.map((f, i) => (
          <View
            key={i}
            className={`filter-chip ${activeFilter === i ? 'chip-active' : ''}`}
            onClick={() => handleMasteryChange(i, f.value)}
          >
            <Text>{f.label}</Text>
          </View>
        ))}
      </View>

      {/* 学科 + 标签筛选行 */}
      <View className='search-bar'>
        <View className='subject-input-wrap'>
          <Input
            className='subject-input'
            placeholder='按学科筛选'
            value={subjectInput}
            onInput={e => setSubjectInput(e.detail.value)}
            onConfirm={handleSubjectSearch}
            confirmType='search'
          />
          {subjectInput ? (
            <Text className='input-clear' onClick={() => { setSubjectInput(''); applyFilter(filterRef.current.mastery, undefined, filterRef.current.tagId) }}>×</Text>
          ) : null}
        </View>
        <View
          className={`tag-filter-btn ${selectedTag ? 'tag-filter-active' : ''}`}
          onClick={() => setShowTagFilter(true)}
        >
          <Text className='tag-filter-text'>{selectedTag ? selectedTag.name : '知识点'}</Text>
          {selectedTag ? (
            <Text
              className='tag-filter-clear'
              onClick={e => { e.stopPropagation(); handleSelectTag(null) }}
            >×</Text>
          ) : null}
        </View>
      </View>

      {/* 错题列表 */}
      <View className='mistake-list'>
        {mistakes.map(m => {
          const mastery = getMasteryInfo(m.masteryLevel || 0)
          return (
            <View
              key={m.id}
              className='mistake-card'
              onClick={() => Taro.navigateTo({ url: `/pages/mistake-detail/index?id=${m.id}` })}
            >
              <View className='card-header'>
                <Text className={`mastery-badge ${mastery.cls}`}>{mastery.label}</Text>
                {m.subject ? <Text className='subject-tag'>{m.subject}</Text> : null}
              </View>
              <Text className='card-title'>{m.title}</Text>
              {m.imageUrl ? (
                <Image className='card-img' src={m.imageUrl} mode='widthFix' />
              ) : null}
              <View className='card-footer'>
                <View className='mastery-track'>
                  <View className={`mastery-fill ${mastery.cls}`} style={{ width: `${m.masteryLevel || 0}%` }} />
                </View>
                <Text className='mastery-pct'>{m.masteryLevel || 0}%</Text>
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

      {/* 知识点/标签筛选弹窗 */}
      {showTagFilter && (
        <View className='modal-mask' onClick={() => setShowTagFilter(false)}>
          <View className='modal-content' onClick={e => e.stopPropagation()}>
            <View className='modal-header'>
              <Text className='modal-title'>按知识点筛选</Text>
              <Text className='modal-close' onClick={() => setShowTagFilter(false)}>×</Text>
            </View>
            <ScrollView scrollY className='modal-list'>
              <View
                className={`tag-row ${!selectedTag ? 'tag-row-active' : ''}`}
                onClick={() => handleSelectTag(null)}
              >
                <Text className='tag-name'>全部</Text>
              </View>
              {allTags.map(tag => (
                <View
                  key={tag.id}
                  className={`tag-row ${selectedTag?.id === tag.id ? 'tag-row-active' : ''}`}
                  onClick={() => handleSelectTag(tag)}
                >
                  <Text className='tag-name'>{tag.name}</Text>
                  <Text className='tag-type-label'>{{ SUBJECT: '学科', CHAPTER: '章节', KNOWLEDGE: '知识点' }[tag.type] || ''}</Text>
                </View>
              ))}
              {allTags.length === 0 && (
                <View className='modal-empty'>
                  <Text className='modal-empty-text'>暂无标签数据</Text>
                </View>
              )}
            </ScrollView>
          </View>
        </View>
      )}
    </View>
  )
}

export default MistakePage
