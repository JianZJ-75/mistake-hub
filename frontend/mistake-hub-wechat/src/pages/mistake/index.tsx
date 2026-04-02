import { useState, useRef, useEffect, useCallback } from 'react'
import Taro, { useLoad, useDidShow, useReachBottom, usePullDownRefresh } from '@tarojs/taro'
import { View, Text, Image } from '@tarojs/components'
import { mistakeList } from '../../service/mistake'
import { tagTree, tagCustomList } from '../../service/tag'
import { MistakeDetailResp, TagResp } from '../../types'
import { getMasteryInfo } from '../../utils/mistake'
import TagPickerModal from '../../components/TagPickerModal'
import './index.scss'

const PAGE_SIZE = 10

const MASTERY_FILTERS = [
  { label: '全部', value: undefined as number | undefined },
  { label: '未掌握', value: 0 },
  { label: '掌握中', value: 1 },
  { label: '已掌握', value: 2 },
]

/** 递归展开标签树为平铺列表 */
const flattenTags = (tags: TagResp[]): TagResp[] => {

  const result: TagResp[] = []
  for (const tag of tags) {
    result.push(tag)
    if (tag.children && tag.children.length) result.push(...flattenTags(tag.children))
  }
  return result
}

const MistakePage = () => {

  // ===== 列表状态 =====
  const [mistakes, setMistakes] = useState<MistakeDetailResp[]>([])
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)

  // ===== 筛选状态 =====
  const [activeFilter, setActiveFilter] = useState(0)
  const [globalTags, setGlobalTags] = useState<TagResp[]>([])
  const [customTags, setCustomTags] = useState<TagResp[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])
  const [showTagPicker, setShowTagPicker] = useState(false)

  // ===== 防双重请求 ref =====
  const loadingRef = useRef(false)
  const pageNumRef = useRef(1)
  const filterRef = useRef<{ mastery?: number; tagIds?: string }>({})

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleMistakeChanged = useCallback(() => fetchList(1, true), [])

  useEffect(() => {
    tagTree().then(tree => setGlobalTags(tree)).catch(() => {})
    loadCustomTags()
    Taro.eventCenter.on('mistakeChanged', handleMistakeChanged)
    return () => { Taro.eventCenter.off('mistakeChanged', handleMistakeChanged) }
  }, [handleMistakeChanged])

  const loadCustomTags = () => {

    tagCustomList().then(list => setCustomTags(list)).catch(() => {})
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

  const handleMasteryChange = (index: number, value?: number) => {

    setActiveFilter(index)
    filterRef.current = { ...filterRef.current, mastery: value }
    fetchList(1, true)
  }

  /** 弹窗确认选中标签后，应用筛选 */
  const handleTagConfirm = (ids: number[]) => {

    setSelectedTagIds(ids)
    const tagIdsStr = ids.length > 0 ? ids.join(',') : undefined
    filterRef.current = { ...filterRef.current, tagIds: tagIdsStr }
    fetchList(1, true)
  }

  /** 移除某个已选中的标签 */
  const handleRemoveTag = (id: number) => {

    const newIds = selectedTagIds.filter(t => t !== id)
    handleTagConfirm(newIds)
  }

  /** 清空所有标签筛选 */
  const handleClearTags = () => {

    handleTagConfirm([])
  }

  /** 根据 ID 找标签名 */
  const allTagsFlat = [...flattenTags(globalTags), ...customTags]
  const tagNameMap = new Map(allTagsFlat.map(t => [t.id, t.name]))

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

      {/* 标签筛选栏 */}
      <View className='tag-filter-bar'>
        <View className='tag-filter-chips'>
          {selectedTagIds.length === 0 ? (
            <View className='tag-filter-add' onClick={() => setShowTagPicker(true)}>
              <Text className='tag-filter-add-text'>+ 选择标签筛选</Text>
            </View>
          ) : (
            <>
              {selectedTagIds.map(id => (
                <View key={id} className='tag-filter-selected'>
                  <Text className='tag-filter-selected-text'>{tagNameMap.get(id) || id}</Text>
                  <Text className='tag-filter-selected-remove' onClick={() => handleRemoveTag(id)}>×</Text>
                </View>
              ))}
              <View className='tag-filter-add' onClick={() => setShowTagPicker(true)}>
                <Text className='tag-filter-add-text'>+</Text>
              </View>
              <View className='tag-filter-clear' onClick={handleClearTags}>
                <Text className='tag-filter-clear-text'>清空</Text>
              </View>
            </>
          )}
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

      {/* 标签多选弹窗（自动全展开） */}
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
