import { useState } from 'react'
import Taro, { useLoad } from '@tarojs/taro'
import { View, Text, Image, ScrollView } from '@tarojs/components'
import { reviewHistory } from '../../service/review'
import { ReviewRecordResp } from '../../types'
import './index.scss'

const ReviewHistoryPage = () => {

  const [records, setRecords] = useState<ReviewRecordResp[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  useLoad(params => {
    const mistakeId = Number(params.mistakeId)
    if (!mistakeId) {
      Taro.navigateBack()
      return
    }
    fetchHistory(mistakeId)
  })

  const fetchHistory = async (mistakeId: number) => {

    setLoading(true)
    try {
      const res = await reviewHistory(mistakeId)
      setRecords(res || [])
    } catch {
      // request.ts 已处理 toast
    } finally {
      setLoading(false)
    }
  }

  const formatTime = (t: string) => t ? t.replace('T', ' ').substring(0, 16) : ''

  const toggleExpand = (id: number) => {

    setExpandedId(prev => prev === id ? null : id)
  }

  return (
    <View className='history-page'>
      <ScrollView scrollY className='history-scroll'>
        <View className='history-list'>
          {loading && (
            <View className='loading-wrap'>
              <Text className='loading-text'>加载中…</Text>
            </View>
          )}

          {!loading && records.length === 0 && (
            <View className='empty-wrap'>
              <Text className='empty-text'>暂无复习记录</Text>
            </View>
          )}

          {!loading && records.map(r => {
            const expanded = expandedId === r.id
            const delta = r.masteryAfter - r.masteryBefore
            return (
              <View key={r.id} className={`record-card ${expanded ? 'record-card-expanded' : ''}`} onClick={() => toggleExpand(r.id)}>
                <View className='record-header'>
                  <View className='record-header-left'>
                    <Text className={`record-result ${r.isCorrect === 1 ? 'result-correct' : 'result-wrong'}`}>
                      {r.isCorrect === 1 ? '答对 ✓' : '答错 ✗'}
                    </Text>
                    <Text className={`record-delta ${delta >= 0 ? 'delta-up' : 'delta-down'}`}>
                      掌握度 {delta >= 0 ? '+' : ''}{delta}
                    </Text>
                  </View>
                  <View className='record-header-right'>
                    <Text className='record-time'>{formatTime(r.reviewTime)}</Text>
                    <Text className='record-arrow'>{expanded ? '∧' : '∨'}</Text>
                  </View>
                </View>

                {expanded && (
                  <View className='record-detail'>
                    <View className='detail-row'>
                      <Text className='detail-label'>复习阶段</Text>
                      <Text className='detail-value'>{r.reviewStageBefore} → {r.reviewStageAfter}</Text>
                    </View>
                    <View className='detail-row'>
                      <Text className='detail-label'>掌握度</Text>
                      <Text className='detail-value'>{r.masteryBefore}% → {r.masteryAfter}%</Text>
                    </View>

                    {r.note ? (
                      <View className='record-note'>
                        <Text className='note-label'>复习备注</Text>
                        <Text className='note-content'>{r.note}</Text>
                      </View>
                    ) : (
                      <View className='record-note'>
                        <Text className='note-empty'>未填写备注</Text>
                      </View>
                    )}

                    {r.noteImageUrl ? (
                      <Image
                        className='note-image'
                        src={r.noteImageUrl}
                        mode='widthFix'
                        onClick={e => { e.stopPropagation(); Taro.previewImage({ urls: [r.noteImageUrl!], current: r.noteImageUrl }) }}
                      />
                    ) : null}
                  </View>
                )}
              </View>
            )
          })}
        </View>
      </ScrollView>
    </View>
  )
}

export default ReviewHistoryPage
