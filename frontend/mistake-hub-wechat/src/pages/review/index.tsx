import { View, Text } from '@tarojs/components'
import '../../styles/placeholder.scss'

const ReviewPage = () => {

  return (
    <View className='page-container'>
      <View className='placeholder-card'>
        <Text className='placeholder-icon'>📖</Text>
        <Text className='placeholder-title'>复习</Text>
        <Text className='placeholder-desc'>复习执行流程将在迭代 4 实现</Text>
      </View>
    </View>
  )
}

export default ReviewPage
