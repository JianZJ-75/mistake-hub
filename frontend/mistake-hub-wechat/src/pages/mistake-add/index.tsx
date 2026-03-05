import { useState, useEffect } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Textarea, Input, Image, ScrollView } from '@tarojs/components'
import { mistakeAdd } from '../../service/mistake'
import { tagTree } from '../../service/tag'
import { uploadImage } from '../../service/upload'
import { TagResp } from '../../types'
import { flattenTags } from '../../utils/mistake'
import './index.scss'

const TYPE_LABEL: Record<string, string> = { SUBJECT: '学科', CHAPTER: '章节', KNOWLEDGE: '知识点' }
const TYPE_INDENT: Record<string, string> = { SUBJECT: '0', CHAPTER: '32rpx', KNOWLEDGE: '64rpx' }

const MistakeAddPage = () => {
  // ===== 表单状态 =====
  const [title, setTitle] = useState('')
  const [correctAnswer, setCorrectAnswer] = useState('')
  const [errorReason, setErrorReason] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [subject, setSubject] = useState('')
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])

  // ===== 弹窗 / 加载状态 =====
  const [allTags, setAllTags] = useState<TagResp[]>([])
  const [showTagModal, setShowTagModal] = useState(false)
  const [tempTagIds, setTempTagIds] = useState<number[]>([])
  const [uploading, setUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    tagTree().then(tree => setAllTags(flattenTags(tree))).catch(() => {})
  }, [])

  const handleChooseImage = async () => {
    if (uploading) return
    try {
      const res = await Taro.chooseMedia({ count: 1, mediaType: ['image'], sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      const path = res.tempFiles[0].tempFilePath
      setUploading(true)
      const url = await uploadImage(path)
      setImageUrl(url)
    } catch (err: unknown) {
      // 用户主动取消不提示，其他错误（如权限拒绝）给出反馈
      const msg = err instanceof Error ? err.message : String(err ?? '')
      if (msg && !msg.includes('cancel')) {
        Taro.showToast({ title: '选择图片失败', icon: 'none' })
      }
    } finally {
      setUploading(false)
    }
  }

  const openTagModal = () => {
    setTempTagIds([...selectedTagIds])
    setShowTagModal(true)
  }

  const toggleTag = (id: number) => {
    setTempTagIds(prev =>
      prev.includes(id) ? prev.filter(t => t !== id) : [...prev, id]
    )
  }

  const confirmTags = () => {
    setSelectedTagIds(tempTagIds)
    setShowTagModal(false)
  }

  const getSelectedTagNames = () => {
    return allTags.filter(t => selectedTagIds.includes(t.id)).map(t => t.name)
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
        errorReason: errorReason.trim() || undefined,
        imageUrl: imageUrl || undefined,
        subject: subject.trim() || undefined,
        tagIds: selectedTagIds.length ? selectedTagIds : undefined,
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

        {/* 图片上传 */}
        <View className='form-section'>
          <Text className='section-label'>题目图片</Text>
          {imageUrl ? (
            <View className='img-preview-wrap'>
              <Image className='img-preview' src={imageUrl} mode='widthFix' />
              <View className='img-remove' onClick={() => setImageUrl('')}>
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

        {/* 错误原因 */}
        <View className='form-section'>
          <Text className='section-label'>错误原因</Text>
          <Textarea
            className='textarea'
            placeholder='分析错误原因或知识点漏洞（选填）'
            value={errorReason}
            onInput={e => setErrorReason(e.detail.value)}
            autoHeight
            maxlength={2000}
          />
        </View>

        {/* 学科 */}
        <View className='form-section'>
          <Text className='section-label'>学科</Text>
          <Input
            className='input'
            placeholder='如：数学、英语（选填）'
            value={subject}
            onInput={e => setSubject(e.detail.value)}
            maxlength={64}
          />
        </View>

        {/* 标签 */}
        <View className='form-section'>
          <Text className='section-label'>标签</Text>
          <View className='tag-selector' onClick={openTagModal}>
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

        {/* 提交按钮 */}
        <View className={`submit-btn ${submitting ? 'btn-disabled' : ''}`} onClick={submitting ? undefined : handleSubmit}>
          <Text className='submit-text'>{submitting ? '保存中…' : '保存错题'}</Text>
        </View>
      </ScrollView>

      {/* 标签选择弹窗 */}
      {showTagModal && (
        <View className='modal-mask' onClick={() => setShowTagModal(false)}>
          <View className='modal-content' onClick={e => e.stopPropagation()}>
            <View className='modal-header'>
              <Text className='modal-title'>选择标签</Text>
              <Text className='modal-close' onClick={() => setShowTagModal(false)}>×</Text>
            </View>
            <ScrollView scrollY className='modal-list'>
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
              {allTags.length === 0 && (
                <View className='modal-empty'>
                  <Text className='modal-empty-text'>暂无标签，请管理员先添加</Text>
                </View>
              )}
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

export default MistakeAddPage
