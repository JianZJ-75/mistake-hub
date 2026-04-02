import { useState, useEffect } from 'react'
import { View, Text, Input, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { TagResp } from '../../types'
import { cascadeToggleTag } from '../../utils/tagCascade'
import { tagCustomAdd, tagCustomDelete } from '../../service/tag'
import './index.scss'

interface TagPickerModalProps {
  visible: boolean
  title?: string
  tags: TagResp[]
  customTags?: TagResp[]
  mode: 'multi' | 'single'
  autoExpand?: boolean
  selectedIds?: number[]
  selectedId?: number | null
  onConfirm?: (ids: number[]) => void
  onSelect?: (tag: TagResp | null) => void
  onClose: () => void
  onCustomTagsChange?: () => void
}

/** 收集所有有子节点的标签 ID */
const collectExpandableIds = (tags: TagResp[]): Set<number> => {

  const ids = new Set<number>()
  const walk = (nodes: TagResp[]) => {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        ids.add(node.id)
        walk(node.children)
      }
    }
  }
  walk(tags)
  return ids
}

const TagPickerModal = ({
  visible,
  title = '选择标签',
  tags,
  customTags = [],
  mode,
  autoExpand = false,
  selectedIds = [],
  selectedId = null,
  onConfirm,
  onSelect,
  onClose,
  onCustomTagsChange,
}: TagPickerModalProps) => {

  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set())
  const [tempIds, setTempIds] = useState<number[]>([])
  const [newCustomName, setNewCustomName] = useState('')
  const [adding, setAdding] = useState(false)

  useEffect(() => {
    if (visible) {
      if (mode === 'multi') {
        setTempIds([...selectedIds])
      }
      if (autoExpand) {
        setExpandedIds(collectExpandableIds(tags))
      }
    }
  }, [visible, selectedIds, mode, autoExpand, tags])

  const toggleExpand = (id: number) => {

    setExpandedIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const toggleCheck = (id: number) => {

    setTempIds(prev => cascadeToggleTag(tags, prev, id))
  }

  const handleConfirm = () => {

    onConfirm?.(tempIds)
    onClose()
  }

  const handleSingleSelect = (tag: TagResp | null) => {

    onSelect?.(tag)
    onClose()
  }

  const handleAddCustom = async () => {

    const name = newCustomName.trim()
    if (!name) return
    setAdding(true)
    try {
      await tagCustomAdd(name)
      setNewCustomName('')
      onCustomTagsChange?.()
      Taro.showToast({ title: '已添加', icon: 'success', duration: 1000 })
    } catch {
      // request 已 toast
    } finally {
      setAdding(false)
    }
  }

  const handleDeleteCustom = async (id: number) => {

    try {
      await tagCustomDelete(id)
      setTempIds(prev => prev.filter(t => t !== id))
      onCustomTagsChange?.()
      Taro.showToast({ title: '已删除', icon: 'success', duration: 1000 })
    } catch {
      // request 已 toast
    }
  }

  const renderTagNode = (tag: TagResp, depth: number) => {

    const hasChildren = tag.children && tag.children.length > 0
    const expanded = expandedIds.has(tag.id)

    return (
      <View key={tag.id}>
        <View
          className={`tag-picker-modal__row ${
            mode === 'multi' && tempIds.includes(tag.id) ? 'tag-picker-modal__row--active' : ''
          }${mode === 'single' && selectedId === tag.id ? ' tag-picker-modal__row--active' : ''}`}
          style={{ paddingLeft: `${32 + depth * 40}rpx` }}
          onClick={() => {
            if (mode === 'multi') {
              toggleCheck(tag.id)
            } else {
              handleSingleSelect(tag)
            }
          }}
        >
          {/* 展开/收缩箭头 */}
          {hasChildren ? (
            <View
              className='tag-picker-modal__expand-icon'
              onClick={e => { e.stopPropagation(); toggleExpand(tag.id) }}
            >
              <Text className={expanded ? 'tag-picker-modal__arrow-down' : 'tag-picker-modal__arrow-right'} />
            </View>
          ) : (
            <View className='tag-picker-modal__expand-placeholder' />
          )}

          {/* 多选模式：checkbox */}
          {mode === 'multi' && (
            <View className={`tag-picker-modal__checkbox ${tempIds.includes(tag.id) ? 'tag-picker-modal__checkbox--checked' : ''}`} />
          )}

          <Text className='tag-picker-modal__tag-name'>{tag.name}</Text>
        </View>

        {/* 递归渲染子节点 */}
        {hasChildren && expanded && tag.children!.map(child => renderTagNode(child, depth + 1))}
      </View>
    )
  }

  if (!visible) return null

  return (
    <View className='tag-picker-modal__mask' onClick={onClose}>
      <View className='tag-picker-modal__content' onClick={e => e.stopPropagation()}>
        <View className='tag-picker-modal__header'>
          <Text className='tag-picker-modal__title'>{title}</Text>
          <Text className='tag-picker-modal__close' onClick={onClose}>×</Text>
        </View>
        <ScrollView scrollY className='tag-picker-modal__list'>
          {/* 单选模式：「全部」选项 */}
          {mode === 'single' && (
            <View
              className={`tag-picker-modal__row ${selectedId === null ? 'tag-picker-modal__row--active' : ''}`}
              style={{ paddingLeft: '32rpx' }}
              onClick={() => handleSingleSelect(null)}
            >
              <View className='tag-picker-modal__expand-placeholder' />
              <Text className='tag-picker-modal__tag-name'>全部</Text>
            </View>
          )}

          {/* 全局标签树 */}
          {tags.map(tag => renderTagNode(tag, 0))}

          {/* 自定义标签区域 */}
          {customTags.length > 0 && (
            <View>
              <View className='tag-picker-modal__section-title'>
                <Text className='tag-picker-modal__section-text'>自定义标签</Text>
              </View>
              {customTags.map(tag => (
                <View
                  key={tag.id}
                  className={`tag-picker-modal__row ${
                    mode === 'multi' && tempIds.includes(tag.id) ? 'tag-picker-modal__row--active' : ''
                  }${mode === 'single' && selectedId === tag.id ? ' tag-picker-modal__row--active' : ''}`}
                  style={{ paddingLeft: '32rpx' }}
                  onClick={() => {
                    if (mode === 'multi') {
                      toggleCheck(tag.id)
                    } else {
                      handleSingleSelect(tag)
                    }
                  }}
                >
                  <View className='tag-picker-modal__expand-placeholder' />
                  {mode === 'multi' && (
                    <View className={`tag-picker-modal__checkbox ${tempIds.includes(tag.id) ? 'tag-picker-modal__checkbox--checked' : ''}`} />
                  )}
                  <Text className='tag-picker-modal__tag-name'>{tag.name}</Text>
                  <Text
                    className='tag-picker-modal__delete-btn'
                    onClick={e => { e.stopPropagation(); handleDeleteCustom(tag.id) }}
                  >删除</Text>
                </View>
              ))}
            </View>
          )}

          {/* 新建自定义标签 */}
          <View className='tag-picker-modal__custom-add'>
            <Input
              className='tag-picker-modal__custom-input'
              placeholder='输入自定义标签名'
              value={newCustomName}
              onInput={e => setNewCustomName(e.detail.value)}
              maxlength={128}
            />
            <View
              className={`tag-picker-modal__custom-add-btn ${adding ? 'tag-picker-modal__custom-add-btn--disabled' : ''}`}
              onClick={adding ? undefined : handleAddCustom}
            >
              <Text>{adding ? '...' : '添加'}</Text>
            </View>
          </View>

          {tags.length === 0 && customTags.length === 0 && (
            <View className='tag-picker-modal__empty'>
              <Text className='tag-picker-modal__empty-text'>暂无标签</Text>
            </View>
          )}
        </ScrollView>

        {/* 多选模式：底部按钮 */}
        {mode === 'multi' && (
          <View className='tag-picker-modal__footer'>
            <View className='tag-picker-modal__btn tag-picker-modal__btn--cancel' onClick={onClose}>
              <Text>取消</Text>
            </View>
            <View className='tag-picker-modal__btn tag-picker-modal__btn--confirm' onClick={handleConfirm}>
              <Text>确认（{tempIds.length}）</Text>
            </View>
          </View>
        )}
      </View>
    </View>
  )
}

export default TagPickerModal
