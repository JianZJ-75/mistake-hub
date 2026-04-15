import { useState, useCallback, useRef } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Image, Textarea, ScrollView } from '@tarojs/components'
import { reviewToday, reviewSubmit, reviewSkip } from '../../service/review'
import { uploadImage } from '../../service/upload'
import { ReviewTaskResp, ReviewSubmitResp } from '../../types'
import { getMasteryInfo } from '../../utils/mistake'
import './index.scss'

const ReviewPage = () => {

  // ===== 任务列表状态 =====
  const [tasks, setTasks] = useState<ReviewTaskResp[]>([])
  const [loading, setLoading] = useState(true)

  // ===== 展开/交互状态 =====
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [answerVisible, setAnswerVisible] = useState<Record<number, boolean>>({})
  const [submitting, setSubmitting] = useState(false)
  const [feedbackMap, setFeedbackMap] = useState<Record<number, ReviewSubmitResp>>({})

  // ===== 备注状态 =====
  const [noteText, setNoteText] = useState<Record<number, string>>({})
  const [noteImage, setNoteImage] = useState<Record<number, string>>({})
  const [uploading, setUploading] = useState(false)
  const pickingImageRef = useRef(false)

  const fetchTasks = useCallback(async () => {

    setLoading(true)
    try {
      const res = await reviewToday()
      setTasks(res || [])
    } catch {
      // request.ts 已处理 toast
    } finally {
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    if (pickingImageRef.current) {
      pickingImageRef.current = false
      return
    }
    fetchTasks()
    setExpandedId(null)
    setAnswerVisible({})
    setFeedbackMap({})
    setNoteText({})
    setNoteImage({})
  })

  const handleExpand = (mistakeId: number) => {

    if (expandedId === mistakeId) {
      setExpandedId(null)
    } else {
      setExpandedId(mistakeId)
    }
  }

  const revealAnswer = (mistakeId: number) => {

    setAnswerVisible(prev => ({ ...prev, [mistakeId]: true }))
  }

  const handleChooseNoteImage = async (mistakeId: number) => {

    if (uploading) return
    try {
      pickingImageRef.current = true
      const res = await Taro.chooseMedia({ count: 1, mediaType: ['image'], sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      const path = res.tempFiles[0].tempFilePath
      setUploading(true)
      const url = await uploadImage(path)
      setNoteImage(prev => ({ ...prev, [mistakeId]: url }))
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err !== null && err !== undefined ? err : '')
      if (msg && !msg.includes('cancel')) {
        Taro.showToast({ title: '选择图片失败', icon: 'none' })
      }
    } finally {
      setUploading(false)
    }
  }

  const handleSubmit = async (mistakeId: number, isCorrect: boolean) => {

    if (submitting) return
    setSubmitting(true)
    try {
      const note = noteText[mistakeId]?.trim() || undefined
      const noteImg = noteImage[mistakeId] || undefined
      const resp = await reviewSubmit(mistakeId, isCorrect, note, noteImg)
      // 更新本地任务状态
      setTasks(prev => prev.map(t =>
        t.mistakeId === mistakeId ? { ...t, planStatus: 'COMPLETED' } : t
      ))
      // 记录反馈数据
      setFeedbackMap(prev => ({ ...prev, [mistakeId]: resp }))
      // 短暂展示反馈后折叠
      setTimeout(() => {
        setExpandedId(null)
      }, 1500)
    } catch {
      // request.ts 已处理 toast
    } finally {
      setSubmitting(false)
    }
  }

  const handleSkip = async (mistakeId: number) => {

    if (submitting) return
    setSubmitting(true)
    try {
      await reviewSkip(mistakeId)
      setTasks(prev => prev.map(t =>
        t.mistakeId === mistakeId ? { ...t, planStatus: 'SKIPPED' } : t
      ))
      setExpandedId(null)
    } catch {
      // request.ts 已处理 toast
    } finally {
      setSubmitting(false)
    }
  }

  // ===== 统计数据 =====
  const total = tasks.length
  const completed = tasks.filter(t => t.planStatus === 'COMPLETED').length
  const skipped = tasks.filter(t => t.planStatus === 'SKIPPED').length
  const pending = total - completed - skipped
  const allDone = total > 0 && pending === 0

  // ===== 完成总结数据 =====
  const feedbackList = Object.values(feedbackMap)
  const correctCount = feedbackList.filter(f => f.masteryAfter > f.masteryBefore).length
  const avgDelta = feedbackList.length > 0
    ? feedbackList.reduce((sum, f) => sum + (f.masteryAfter - f.masteryBefore), 0) / feedbackList.length
    : 0

  return (
    <View className='review-page'>
      {/* 顶部进度条 */}
      <View className='progress-bar-section'>
        <View className='progress-info'>
          <Text className='progress-text'>今日进度</Text>
          <Text className='progress-num'>{completed}/{total}</Text>
        </View>
        <View className='progress-track'>
          <View
            className='progress-fill'
            style={{ width: total > 0 ? `${(completed / total) * 100}%` : '0%' }}
          />
        </View>
      </View>

      {/* 任务列表 */}
      <ScrollView scrollY className='task-scroll'>
        {loading && (
          <View className='loading-wrap'>
            <Text className='loading-text'>加载中…</Text>
          </View>
        )}

        {!loading && total === 0 && (
          <View className='empty-wrap'>
            <Text className='empty-icon'>📚</Text>
            <Text className='empty-title'>今日无复习任务</Text>
            <Text className='empty-desc'>去录入新错题，明天就会出现在这里</Text>
          </View>
        )}

        {!loading && allDone && total > 0 && (
          <View className='summary-card'>
            <Text className='summary-icon'>🎉</Text>
            <Text className='summary-title'>今日复习完成！</Text>
            <View className='summary-stats'>
              <View className='summary-stat'>
                <Text className='summary-stat-num'>{completed}</Text>
                <Text className='summary-stat-label'>已完成</Text>
              </View>
              <View className='summary-stat'>
                <Text className='summary-stat-num'>{skipped}</Text>
                <Text className='summary-stat-label'>已跳过</Text>
              </View>
              {feedbackList.length > 0 && (
                <>
                  <View className='summary-stat'>
                    <Text className='summary-stat-num'>
                      {feedbackList.length > 0 ? Math.round((correctCount / feedbackList.length) * 100) : 0}%
                    </Text>
                    <Text className='summary-stat-label'>正确率</Text>
                  </View>
                  <View className='summary-stat'>
                    <Text className='summary-stat-num'>
                      {avgDelta >= 0 ? '+' : ''}{Math.round(avgDelta)}
                    </Text>
                    <Text className='summary-stat-label'>平均掌握度</Text>
                  </View>
                </>
              )}
            </View>
          </View>
        )}

        {!loading && tasks.map(task => {
          const mastery = getMasteryInfo(task.masteryLevel || 0)
          const isExpanded = expandedId === task.mistakeId
          const isDone = task.planStatus === 'COMPLETED'
          const isSkipped = task.planStatus === 'SKIPPED'
          const feedback = feedbackMap[task.mistakeId]
          const isAnswerRevealed = answerVisible[task.mistakeId] || false

          return (
            <View key={task.mistakeId} className='task-card'>
              {/* 卡片头部（点击展开） */}
              <View
                className={`task-header ${isDone ? 'task-done' : ''} ${isSkipped ? 'task-skipped' : ''}`}
                onClick={() => !isDone && !isSkipped && handleExpand(task.mistakeId)}
              >
                <View className='task-header-left'>
                  <Text className={`status-dot ${isDone ? 'dot-done' : isSkipped ? 'dot-skipped' : 'dot-pending'}`} />
                  <Text className={`task-title ${isDone || isSkipped ? 'task-title-muted' : ''}`}>
                    {task.title}
                  </Text>
                </View>
                <View className='task-header-right'>
                  <Text className={`mastery-badge ${mastery.cls}`}>{mastery.label}</Text>
                  {isDone && <Text className='status-tag tag-done'>已完成</Text>}
                  {isSkipped && <Text className='status-tag tag-skipped'>已跳过</Text>}
                </View>
              </View>

              {/* 标签 */}
              {task.tags && task.tags.length > 0 && (
                <View className='task-tags'>
                  {task.tags.map(tag => (
                    <Text key={tag.id} className='tag-chip'>{tag.name}</Text>
                  ))}
                </View>
              )}

              {/* 展开详情区 */}
              {isExpanded && !isDone && !isSkipped && (
                <View className='task-detail'>
                  {/* 题目 + 题目图片 */}
                  <Text className='detail-title'>{task.title}</Text>
                  {task.titleImageUrl && (
                    <Image
                      className='detail-img'
                      src={task.titleImageUrl}
                      mode='widthFix'
                      onClick={() => Taro.previewImage({ urls: [task.titleImageUrl!], current: task.titleImageUrl })}
                    />
                  )}

                  {/* 掌握度进度条 */}
                  <View className='depth-info'>
                    <Text className='depth-label'>掌握深度：{mastery.depthText}</Text>
                    <View className='depth-bar'>
                      {[1, 2, 3, 4, 5].map(i => (
                        <View key={i} className={`depth-seg ${i <= mastery.depthLevel ? 'seg-filled' : 'seg-empty'}`} />
                      ))}
                    </View>
                  </View>

                  {/* 查看答案按钮 / 答案区域 */}
                  {!isAnswerRevealed ? (
                    <View className='reveal-btn' onClick={() => revealAnswer(task.mistakeId)}>
                      <Text className='reveal-btn-text'>查看答案</Text>
                    </View>
                  ) : (
                    <View className='answer-section'>
                      {/* 正确答案 */}
                      {task.correctAnswer ? (
                        <View className='answer-block'>
                          <Text className='answer-label'>正确答案</Text>
                          <Text className='answer-content answer-correct'>{task.correctAnswer}</Text>
                        </View>
                      ) : null}
                      {task.answerImageUrl ? (
                        <Image
                          className='detail-img'
                          src={task.answerImageUrl}
                          mode='widthFix'
                          onClick={() => Taro.previewImage({ urls: [task.answerImageUrl!], current: task.answerImageUrl })}
                        />
                      ) : null}

                      {/* 查看复习历史 */}
                      <Text
                        className='history-link'
                        onClick={() => Taro.navigateTo({ url: `/pages/review-history/index?mistakeId=${task.mistakeId}` })}
                      >
                        查看复习历史 ›
                      </Text>

                      {/* 备注输入 */}
                      <View className='note-section'>
                        <Text className='note-label'>答题备注（选填）</Text>
                        <Textarea
                          className='note-textarea'
                          placeholder='记录本次答题的想法...'
                          value={noteText[task.mistakeId] || ''}
                          onInput={e => setNoteText(prev => ({ ...prev, [task.mistakeId]: e.detail.value }))}
                          autoHeight
                          maxlength={500}
                        />
                        {noteImage[task.mistakeId] ? (
                          <View className='note-img-wrap'>
                            <Image className='note-img' src={noteImage[task.mistakeId]} mode='widthFix' />
                            <View className='note-img-remove' onClick={() => setNoteImage(prev => ({ ...prev, [task.mistakeId]: '' }))}>
                              <Text className='note-img-remove-text'>×</Text>
                            </View>
                          </View>
                        ) : (
                          <Text className='note-upload-link' onClick={() => handleChooseNoteImage(task.mistakeId)}>
                            {uploading ? '上传中…' : '+ 添加图片'}
                          </Text>
                        )}
                      </View>

                      {/* 作答按钮 */}
                      <View className='action-row'>
                        <View
                          className='answer-btn btn-correct'
                          onClick={() => handleSubmit(task.mistakeId, true)}
                        >
                          <Text className='answer-btn-text'>答对 ✓</Text>
                        </View>
                        <View
                          className='answer-btn btn-wrong'
                          onClick={() => handleSubmit(task.mistakeId, false)}
                        >
                          <Text className='answer-btn-text'>答错 ✗</Text>
                        </View>
                      </View>
                      <View className='skip-row'>
                        <Text className='skip-link' onClick={() => handleSkip(task.mistakeId)}>跳过</Text>
                      </View>
                    </View>
                  )}
                </View>
              )}

              {/* 提交反馈 */}
              {isDone && feedback && (
                <View className='feedback-row'>
                  <Text className='feedback-text'>
                    掌握度 {feedback.masteryBefore}% → {feedback.masteryAfter}%
                    （{feedback.masteryAfter >= feedback.masteryBefore ? '+' : ''}{feedback.masteryAfter - feedback.masteryBefore}）
                  </Text>
                </View>
              )}
            </View>
          )
        })}
      </ScrollView>
    </View>
  )
}

export default ReviewPage
