import { useState, useEffect } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Textarea, Image, ScrollView } from '@tarojs/components'
import { mistakeAdd } from '../../service/mistake'
import { tagTree, tagCustomList } from '../../service/tag'
import { uploadImage } from '../../service/upload'
import { TagResp } from '../../types'
import { findTagNamesByIds } from '../../utils/mistake'
import TagPickerModal from '../../components/TagPickerModal'
import './index.scss'

const MistakeAddPage = () => {
  // ===== 表���状态 =====
  const [title, setTitle] = useState('')
  const [correctAnswer, setCorrectAnswer] = useState('')
  const [titleImageUrl, setTitleImageUrl] = useState('')
  const [answerImageUrl, setAnswerImageUrl] = useState('')
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])
  const [note, setNote] = useState('')
  const [noteImageUrl, setNoteImageUrl] = useState('')

  // ===== 弹窗 / 加载状态 =====
  const [allTags, setAllTags] = useState<TagResp[]>([])
  const [customTags, setCustomTags] = useState<TagResp[]>([])
  const [showTagModal, setShowTagModal] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    tagTree().then(tree => setAllTags(tree)).catch(() => {})
    loadCustomTags()
  }, [])

  const loadCustomTags = () => {
    tagCustomList().then(list => setCustomTags(list)).catch(() => {})
  }

  const handleChooseImage = async (setter: (url: string) => void) => {
    if (uploading) return
    try {
      const res = await Taro.chooseMedia({ count: 1, mediaType: ['image'], sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      const path = res.tempFiles[0].tempFilePath
      setUploading(true)
      const url = await uploadImage(path)
      setter(url)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err !== null && err !== undefined ? err : '')
      if (msg && !msg.includes('cancel')) {
        Taro.showToast({ title: '选择图片失败', icon: 'none' })
      }
    } finally {
      setUploading(false)
    }
  }

  const getSelectedTagNames = () => {
    const allFlat = [...allTags]
    return findTagNamesByIds([...allFlat, ...customTags], selectedTagIds)
  }

  const handleSubmit = async () => {
    if (!title.trim()) {
      Taro.showToast({ title: '题干不能为空', icon: 'none' })
      return
    }
    setSubmitting(true)
    try {
      await mistakeAdd({
        title: title.trim(),
        correctAnswer: correctAnswer.trim() || undefined,
        titleImageUrl: titleImageUrl || undefined,
        answerImageUrl: answerImageUrl || undefined,
        tagIds: selectedTagIds.length ? selectedTagIds : undefined,
        note: note.trim() || undefined,
        noteImageUrl: noteImageUrl || undefined,
      })
      Taro.showToast({ title: '录入成功', icon: 'success' })
      Taro.eventCenter.trigger('mistakeChanged')
      setTimeout(() => Taro.navigateBack(), 1200)
    } catch {
      // 错误已 toast
      setSubmitting(false)
    }
  }

  return (
    <View className='add-page'>
      <ScrollView scrollY className='form-scroll'>
        {/* 题干 */}
        <View className='form-section'>
          <Text className='section-label'>题干 <Text className='required'>*</Text></Text>
          <Textarea
            className='textarea'
            placeholder='请输入题目内容'
            value={title}
            onInput={e => setTitle(e.detail.value)}
            autoHeight
            maxlength={2000}
          />
        </View>

        {/* 题目图片 */}
        <View className='form-section'>
          <Text className='section-label'>题目图片</Text>
          {titleImageUrl ? (
            <View className='img-preview-wrap'>
              <Image className='img-preview' src={titleImageUrl} mode='widthFix' />
              <View className='img-remove' onClick={() => setTitleImageUrl('')}>
                <Text className='img-remove-text'>×</Text>
              </View>
            </View>
          ) : (
            <View className={`img-picker ${uploading ? 'img-picker-loading' : ''}`} onClick={() => handleChooseImage(setTitleImageUrl)}>
              <Text className='img-picker-icon'>{uploading ? '上传中…' : '+'}</Text>
              <Text className='img-picker-hint'>{uploading ? '' : '拍照 / 相册'}</Text>
            </View>
          )}
        </View>

        {/* 正确答案 */}
        <View className='form-section'>
          <Text className='section-label'>正确答案</Text>
          <Textarea
            className='textarea'
            placeholder='请输入正确答案（选填）'
            value={correctAnswer}
            onInput={e => setCorrectAnswer(e.detail.value)}
            autoHeight
            maxlength={2000}
          />
        </View>

        {/* 答案图片 */}
        <View className='form-section'>
          <Text className='section-label'>答案图片</Text>
          {answerImageUrl ? (
            <View className='img-preview-wrap'>
              <Image className='img-preview' src={answerImageUrl} mode='widthFix' />
              <View className='img-remove' onClick={() => setAnswerImageUrl('')}>
                <Text className='img-remove-text'>×</Text>
              </View>
            </View>
          ) : (
            <View className={`img-picker ${uploading ? 'img-picker-loading' : ''}`} onClick={() => handleChooseImage(setAnswerImageUrl)}>
              <Text className='img-picker-icon'>{uploading ? '上传中…' : '+'}</Text>
              <Text className='img-picker-hint'>{uploading ? '' : '拍照 / 相册'}</Text>
            </View>
          )}
        </View>

        {/* 标签 */}
        <View className='form-section'>
          <Text className='section-label'>标签</Text>
          <View className='tag-selector' onClick={() => setShowTagModal(true)}>
            {(() => {
              const names = getSelectedTagNames()
              return names.length > 0 ? (
                <View className='tag-chips'>
                  {names.map((name, i) => (
                    <Text key={i} className='tag-chip'>{name}</Text>
                  ))}
                </View>
              ) : (
                <Text className='tag-placeholder'>点击选择标签（选填）</Text>
              )
            })()}
            <Text className='tag-arrow'>›</Text>
          </View>
        </View>

        {/* 录入备注 */}
        <View className='form-section'>
          <Text className='section-label'>录入备注</Text>
          <Textarea
            className='textarea'
            placeholder='记录本次对该题的分析（选填）'
            value={note}
            onInput={e => setNote(e.detail.value)}
            autoHeight
            maxlength={2000}
          />
        </View>

        {/* 备注图片 */}
        <View className='form-section'>
          <Text className='section-label'>备注图片</Text>
          {noteImageUrl ? (
            <View className='img-preview-wrap'>
              <Image className='img-preview' src={noteImageUrl} mode='widthFix' />
              <View className='img-remove' onClick={() => setNoteImageUrl('')}>
                <Text className='img-remove-text'>×</Text>
              </View>
            </View>
          ) : (
            <View className={`img-picker ${uploading ? 'img-picker-loading' : ''}`} onClick={() => handleChooseImage(setNoteImageUrl)}>
              <Text className='img-picker-icon'>{uploading ? '上传中…' : '+'}</Text>
              <Text className='img-picker-hint'>{uploading ? '' : '拍照 / 相册'}</Text>
            </View>
          )}
        </View>

        {/* 提交按钮 */}
        <View className={`submit-btn ${submitting ? 'btn-disabled' : ''}`} onClick={submitting ? undefined : handleSubmit}>
          <Text className='submit-text'>{submitting ? '保存中…' : '保存错题'}</Text>
        </View>
      </ScrollView>

      {/* 标签选择弹窗 */}
      <TagPickerModal
        visible={showTagModal}
        tags={allTags}
        customTags={customTags}
        mode='multi'
        selectedIds={selectedTagIds}
        onConfirm={ids => setSelectedTagIds(ids)}
        onClose={() => setShowTagModal(false)}
        onCustomTagsChange={loadCustomTags}
      />
    </View>
  )
}

export default MistakeAddPage
