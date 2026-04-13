import { useState, useRef } from 'react'
import Taro, { useLoad } from '@tarojs/taro'
import { View, Text, Textarea, Image, ScrollView } from '@tarojs/components'
import { mistakeDetail, mistakeModify, mistakeDelete } from '../../service/mistake'
import { tagTree, tagCustomList } from '../../service/tag'
import { uploadImage } from '../../service/upload'
import { MistakeDetailResp, TagResp } from '../../types'
import { findTagNamesByIds, getMasteryInfo } from '../../utils/mistake'
import TagPickerModal from '../../components/TagPickerModal'
import './index.scss'

const STAGE_INTERVALS = [0, 1, 2, 4, 7, 15, 30]

interface FormState {
  title: string
  correctAnswer: string
  titleImageUrl: string
  answerImageUrl: string
  tagIds: number[]
}

const MistakeDetailPage = () => {

  const [detail, setDetail] = useState<MistakeDetailResp | null>(null)
  const [editMode, setEditMode] = useState(false)
  const [form, setForm] = useState<FormState>({ title: '', correctAnswer: '', titleImageUrl: '', answerImageUrl: '', tagIds: [] })
  const [allTags, setAllTags] = useState<TagResp[]>([])
  const [customTags, setCustomTags] = useState<TagResp[]>([])
  const [showTagModal, setShowTagModal] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const idRef = useRef(0)

  useLoad(params => {
    const id = Number(params.id)
    idRef.current = id
    loadDetail(id)
    tagTree().then(tree => setAllTags(tree)).catch(() => {})
    loadCustomTags()
  })

  const loadCustomTags = () => {

    tagCustomList().then(list => setCustomTags(list)).catch(() => {})
  }

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
      titleImageUrl: detail.titleImageUrl || '',
      answerImageUrl: detail.answerImageUrl || '',
      tagIds: detail.tags ? detail.tags.map(t => t.id) : [],
    })
    setEditMode(true)
  }

  const handleChooseImage = async (field: keyof Pick<FormState, 'titleImageUrl' | 'answerImageUrl'>) => {

    if (uploading) return
    try {
      const res = await Taro.chooseMedia({ count: 1, mediaType: ['image'], sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      const path = res.tempFiles[0].tempFilePath
      setUploading(true)
      const url = await uploadImage(path)
      setForm(prev => ({ ...prev, [field]: url }))
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err !== null && err !== undefined ? err : '')
      if (msg && !msg.includes('cancel')) {
        Taro.showToast({ title: '选择图片失败', icon: 'none' })
      }
    } finally {
      setUploading(false)
    }
  }

  const handleSave = async () => {

    if (!form.title.trim()) {
      Taro.showToast({ title: '题干不能为空', icon: 'none' })
      return
    }
    setSubmitting(true)
    try {
      await mistakeModify({
        id: idRef.current,
        title: form.title.trim(),
        correctAnswer: form.correctAnswer.trim(),
        titleImageUrl: form.titleImageUrl,
        answerImageUrl: form.answerImageUrl,
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

    findTagNamesByIds([...allTags, ...customTags], ids)

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
              {form.titleImageUrl ? (
                <View className='img-preview-wrap'>
                  <Image className='img-preview' src={form.titleImageUrl} mode='widthFix' />
                  <View className='img-remove' onClick={() => setForm(prev => ({ ...prev, titleImageUrl: '' }))}>
                    <Text className='img-remove-text'>×</Text>
                  </View>
                </View>
              ) : (
                <View className={`img-picker ${uploading ? 'img-picker-loading' : ''}`} onClick={() => handleChooseImage('titleImageUrl')}>
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
              <Text className='section-label'>答案图片</Text>
              {form.answerImageUrl ? (
                <View className='img-preview-wrap'>
                  <Image className='img-preview' src={form.answerImageUrl} mode='widthFix' />
                  <View className='img-remove' onClick={() => setForm(prev => ({ ...prev, answerImageUrl: '' }))}>
                    <Text className='img-remove-text'>×</Text>
                  </View>
                </View>
              ) : (
                <View className={`img-picker ${uploading ? 'img-picker-loading' : ''}`} onClick={() => handleChooseImage('answerImageUrl')}>
                  <Text className='img-picker-icon'>{uploading ? '上传中…' : '+'}</Text>
                  <Text className='img-picker-hint'>{uploading ? '' : '拍照 / 相册'}</Text>
                </View>
              )}
            </View>

            <View className='form-section'>
              <Text className='section-label'>标签</Text>
              <View className='tag-selector' onClick={() => setShowTagModal(true)}>
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
                <Text className='stage-info'>第 {detail.reviewStage} 阶段（间隔 {STAGE_INTERVALS[detail.reviewStage] !== undefined ? STAGE_INTERVALS[detail.reviewStage] : 30} 天）</Text>
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

            {/* 题目图片 */}
            {detail.titleImageUrl ? (
              <View className='detail-section'>
                <Text className='detail-label'>题目图片</Text>
                <Image
                  className='detail-img'
                  src={detail.titleImageUrl}
                  mode='widthFix'
                  onClick={() => Taro.previewImage({ urls: [detail.titleImageUrl!], current: detail.titleImageUrl })}
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

            {/* 答案图片 */}
            {detail.answerImageUrl ? (
              <View className='detail-section'>
                <Text className='detail-label'>答案图片</Text>
                <Image
                  className='detail-img'
                  src={detail.answerImageUrl}
                  mode='widthFix'
                  onClick={() => Taro.previewImage({ urls: [detail.answerImageUrl!], current: detail.answerImageUrl })}
                />
              </View>
            ) : null}

            {/* 标签 */}
            {(detail.tags ? detail.tags.length : 0) > 0 ? (
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
              <View className='btn-history' onClick={() => Taro.navigateTo({ url: `/pages/review-history/index?mistakeId=${idRef.current}` })}>
                <Text>复习历史</Text>
              </View>
              <View className='btn-delete' onClick={handleDelete}>
                <Text>删除</Text>
              </View>
            </View>
          </View>
        )}
      </ScrollView>

      {/* 标签选择弹窗（编辑模式中使用） */}
      <TagPickerModal
        visible={showTagModal}
        tags={allTags}
        customTags={customTags}
        mode='multi'
        selectedIds={form.tagIds}
        onConfirm={ids => setForm(prev => ({ ...prev, tagIds: ids }))}
        onClose={() => setShowTagModal(false)}
        onCustomTagsChange={loadCustomTags}
      />
    </View>
  )
}

export default MistakeDetailPage
