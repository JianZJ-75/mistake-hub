import { View, Text } from '@tarojs/components'
import './index.scss'

const IndexPage = () => {

  return (
    <View className='page-container'>
      <View className='placeholder-card'>
        <Text className='placeholder-icon'>🏠</Text>
        <Text className='placeholder-title'>首页</Text>
        <Text className='placeholder-desc'>今日复习任务将在迭代 4 实现</Text>
      </View>
    </View>
  )
}

export default IndexPage
