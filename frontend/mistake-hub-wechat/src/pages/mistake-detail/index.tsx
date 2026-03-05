import { useState, useRef } from 'react'
import Taro, { useLoad } from '@tarojs/taro'
import { View, Text, Textarea, Input, Image, ScrollView } from '@tarojs/components'
import { mistakeDetail, mistakeUpdate, mistakeDelete } from '../../service/mistake'
import { tagTree } from '../../service/tag'
import { uploadImage } from '../../service/upload'
import { MistakeDetailResp, TagResp } from '../../types'
import { flattenTags, getMasteryInfo } from '../../utils/mistake'
import './index.scss'

const TYPE_LABEL: Record<string, string> = { SUBJECT: '学科', CHAPTER: '章节', KNOWLEDGE: '知识点' }
const TYPE_INDENT: Record<string, string> = { SUBJECT: '0', CHAPTER: '32rpx', KNOWLEDGE: '64rpx' }

const STAGE_INTERVALS = [0, 1, 2, 4, 7, 15, 30]

interface FormState {
  title: string
  correctAnswer: string
  errorReason: string
  imageUrl: string
  subject: string
  tagIds: number[]
}

const MistakeDetailPage = () => {
  const [detail, setDetail] = useState<MistakeDetailResp | null>(null)
  const [editMode, setEditMode] = useState(false)
  const [form, setForm] = useState<FormState>({ title: '', correctAnswer: '', errorReason: '', imageUrl: '', subject: '', tagIds: [] })
  const [allTags, setAllTags] = useState<TagResp[]>([])
  const [showTagModal, setShowTagModal] = useState(false)
  const [tempTagIds, setTempTagIds] = useState<number[]>([])
  const [uploading, setUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const idRef = useRef(0)

  useLoad(params => {
    const id = Number(params.id)
    idRef.current = id
    loadDetail(id)
    tagTree().then(tree => setAllTags(flattenTags(tree))).catch(() => {})
  })

  const loadDetail = async (id: number, navigateBackOnError = true) => {
    try {
      const res = await mistakeDetail(id)
      setDetail(res)
    } catch {
      if (navigateBackOnError) Taro.navigateBack()
    }
  }

  const enterEdit = () => {
    if (!detail) return
    setForm({
      title: detail.title || '',
      correctAnswer: detail.correctAnswer || '',
      errorReason: detail.errorReason || '',
      imageUrl: detail.imageUrl || '',
      subject: detail.subject || '',
      tagIds: detail.tags?.map(t => t.id) || [],
    })
    setEditMode(true)
  }

  const handleChooseImage = async () => {
    if (uploading) return
    try {
      const res = await Taro.chooseMedia({ count: 1, mediaType: ['image'], sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      const path = res.tempFiles[0].tempFilePath
      setUploading(true)
      const url = await uploadImage(path)
      setForm(prev => ({ ...prev, imageUrl: url }))
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err ?? '')
      if (msg && !msg.includes('cancel')) {
        Taro.showToast({ title: '选择图片失败', icon: 'none' })
      }
    } finally {
      setUploading(false)
    }
  }

  const openTagModal = () => {
    setTempTagIds([...form.tagIds])
    setShowTagModal(true)
  }

  const toggleTag = (id: number) => {
    setTempTagIds(prev => prev.includes(id) ? prev.filter(t => t !== id) : [...prev, id])
  }

  const confirmTags = () => {
    setForm(prev => ({ ...prev, tagIds: tempTagIds }))
    setShowTagModal(false)
  }

  const handleSave = async () => {
    if (!form.title.trim()) {
      Taro.showToast({ title: '题干不能为空', icon: 'none' })
      return
    }
    setSubmitting(true)
    try {
      await mistakeUpdate({
        id: idRef.current,
        title: form.title.trim(),
        correctAnswer: form.correctAnswer.trim() || undefined,
        errorReason: form.errorReason.trim() || undefined,
        imageUrl: form.imageUrl || undefined,
        subject: form.subject.trim() || undefined,
        tagIds: form.tagIds,
      })
      Taro.showToast({ title: '保存成功', icon: 'success' })
      Taro.eventCenter.trigger('mistakeChanged')
      await loadDetail(idRef.current, false)
      setEditMode(false)
    } catch {
      // 错误已 toast
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = () => {
    Taro.showModal({
      title: '确认删除',
      content: '删除后无法恢复，确认吗？',
      success: async res => {
        if (!res.confirm) return
        try {
          await mistakeDelete(idRef.current)
          Taro.eventCenter.trigger('mistakeChanged')
          Taro.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => Taro.navigateBack(), 1000)
        } catch {
          // 错误已 toast
        }
      },
    })
  }

  const getTagNames = (ids: number[]) =>
    allTags.filter(t => ids.includes(t.id)).map(t => t.name)

  if (!detail) {
    return (
      <View className='loading-page'>
        <Text className='loading-text'>加载中…</Text>
      </View>
    )
  }

  const mastery = getMasteryInfo(detail.masteryLevel || 0)

  return (
    <View className='detail-page'>
      <ScrollView scrollY className='detail-scroll'>
        {editMode ? (
          /* 编辑模式 */
          <View>
            <View className='form-section'>
              <Text className='section-label'>题干 <Text className='required'>*</Text></Text>
              <Textarea
                className='textarea'
                value={form.title}
                onInput={e => setForm(prev => ({ ...prev, title: e.detail.value }))}
                autoHeight
                maxlength={2000}
              />
            </View>

            <View className='form-section'>
              <Text className='section-label'>题目图片</Text>
              {form.imageUrl ? (
                <View className='img-preview-wrap'>
                  <Image className='img-preview' src={form.imageUrl} mode='widthFix' />
                  <View className='img-remove' onClick={() => setForm(prev => ({ ...prev, imageUrl: '' }))}>
                    <Text className='img-remove-text'>×</Text>
                  </View>
                </View>
              ) : (
                <View className={`img-picker ${uploading ? 'img-picker-loading' : ''}`} onClick={handleChooseImage}>
                  <Text className='img-picker-icon'>{uploading ? '上传中…' : '+'}</Text>
                  <Text className='img-picker-hint'>{uploading ? '' : '拍照 / 相册'}</Text>
                </View>
              )}
            </View>

            <View className='form-section'>
              <Text className='section-label'>正确答案</Text>
              <Textarea
                className='textarea'
                value={form.correctAnswer}
                onInput={e => setForm(prev => ({ ...prev, correctAnswer: e.detail.value }))}
                autoHeight
                maxlength={2000}
              />
            </View>

            <View className='form-section'>
              <Text className='section-label'>错误原因</Text>
              <Textarea
                className='textarea'
                value={form.errorReason}
                onInput={e => setForm(prev => ({ ...prev, errorReason: e.detail.value }))}
                autoHeight
                maxlength={2000}
              />
            </View>

            <View className='form-section'>
              <Text className='section-label'>学科</Text>
              <Input
                className='input'
                value={form.subject}
                onInput={e => setForm(prev => ({ ...prev, subject: e.detail.value }))}
                maxlength={64}
              />
            </View>

            <View className='form-section'>
              <Text className='section-label'>标签</Text>
              <View className='tag-selector' onClick={openTagModal}>
                {form.tagIds.length > 0 ? (
                  <View className='tag-chips'>
                    {getTagNames(form.tagIds).map((name, i) => (
                      <Text key={i} className='tag-chip'>{name}</Text>
                    ))}
                  </View>
                ) : (
                  <Text className='tag-placeholder'>点击选择标签</Text>
                )}
                <Text className='tag-arrow'>›</Text>
              </View>
            </View>

            <View className='edit-actions'>
              <View className='btn-cancel' onClick={() => setEditMode(false)}>
                <Text>取消</Text>
              </View>
              <View className={`btn-save ${submitting ? 'btn-disabled' : ''}`} onClick={submitting ? undefined : handleSave}>
                <Text>{submitting ? '保存中…' : '保存'}</Text>
              </View>
            </View>
          </View>
        ) : (
          /* 查看模式 */
          <View>
            {/* 头部状态信息 */}
            <View className='detail-header'>
              <View className='status-row'>
                <Text className={`mastery-badge ${mastery.cls}`}>{mastery.label}</Text>
                {detail.subject ? <Text className='subject-tag'>{detail.subject}</Text> : null}
                <Text className='stage-info'>第 {detail.reviewStage} 阶段（间隔 {STAGE_INTERVALS[detail.reviewStage] ?? 30} 天）</Text>
              </View>
              <View className='mastery-row'>
                <View className='mastery-track'>
                  <View className={`mastery-fill ${mastery.cls}`} style={{ width: `${detail.masteryLevel || 0}%` }} />
                </View>
                <Text className='mastery-pct'>掌握度 {detail.masteryLevel || 0}%</Text>
              </View>
            </View>

            {/* 题干 */}
            <View className='detail-section'>
              <Text className='detail-label'>题干</Text>
              <Text className='detail-text'>{detail.title}</Text>
            </View>

            {/* 图片 */}
            {detail.imageUrl ? (
              <View className='detail-section'>
                <Text className='detail-label'>图片</Text>
                <Image
                  className='detail-img'
                  src={detail.imageUrl}
                  mode='widthFix'
                  onClick={() => Taro.previewImage({ urls: [detail.imageUrl!], current: detail.imageUrl })}
                />
              </View>
            ) : null}

            {/* 正确答案 */}
            {detail.correctAnswer ? (
              <View className='detail-section'>
                <Text className='detail-label'>正确答案</Text>
                <Text className='detail-text answer-text'>{detail.correctAnswer}</Text>
              </View>
            ) : null}

            {/* 错误原因 */}
            {detail.errorReason ? (
              <View className='detail-section'>
                <Text className='detail-label'>错误原因</Text>
                <Text className='detail-text'>{detail.errorReason}</Text>
              </View>
            ) : null}

            {/* 标签 */}
            {(detail.tags?.length ?? 0) > 0 ? (
              <View className='detail-section'>
                <Text className='detail-label'>标签</Text>
                <View className='tag-chips'>
                  {detail.tags!.map(t => (
                    <Text key={t.id} className='tag-chip'>{t.name}</Text>
                  ))}
                </View>
              </View>
            ) : null}

            {/* 操作按钮 */}
            <View className='view-actions'>
              <View className='btn-edit' onClick={enterEdit}>
                <Text>编辑</Text>
              </View>
              <View className='btn-delete' onClick={handleDelete}>
                <Text>删除</Text>
              </View>
            </View>
          </View>
        )}
      </ScrollView>

      {/* 标签选择弹窗（编辑模式中使用） */}
      {showTagModal && (
        <View className='modal-mask' onClick={() => setShowTagModal(false)}>
          <View className='modal-content' onClick={e => e.stopPropagation()}>
            <View className='modal-header'>
              <Text className='modal-title'>选择标签</Text>
              <Text className='modal-close' onClick={() => setShowTagModal(false)}>×</Text>
            </View>
            <ScrollView scrollY className='modal-list'>
              {allTags.length === 0 && (
                <View className='modal-empty'>
                  <Text className='modal-empty-text'>暂无标签，请管理员先添加</Text>
                </View>
              )}
              {allTags.map(tag => (
                <View
                  key={tag.id}
                  className={`tag-row ${tempTagIds.includes(tag.id) ? 'tag-row-active' : ''}`}
                  style={{ paddingLeft: TYPE_INDENT[tag.type] || '0' }}
                  onClick={() => toggleTag(tag.id)}
                >
                  <View className={`checkbox ${tempTagIds.includes(tag.id) ? 'checkbox-checked' : ''}`} />
                  <Text className='tag-name'>{tag.name}</Text>
                  <Text className='tag-type-label'>{TYPE_LABEL[tag.type] || ''}</Text>
                </View>
              ))}
            </ScrollView>
            <View className='modal-footer'>
              <View className='modal-btn modal-btn-cancel' onClick={() => setShowTagModal(false)}>
                <Text>取消</Text>
              </View>
              <View className='modal-btn modal-btn-confirm' onClick={confirmTags}>
                <Text>确认（{tempTagIds.length}）</Text>
              </View>
            </View>
          </View>
        </View>
      )}
    </View>
  )
}

export default MistakeDetailPage
