import { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import { getToken } from '../../utils/request'
import '../../styles/placeholder.scss'

const ProfilePage = () => {

  const [isLoggedIn, setIsLoggedIn] = useState(Boolean(getToken()))

  // 每次页面显示时重新检查 token（登录完成后切到本页能立即更新）
  useEffect(() => {
    setIsLoggedIn(Boolean(getToken()))
  }, [])

  return (
    <View className='page-container'>
      <View className='placeholder-card'>
        <Text className='placeholder-icon'>👤</Text>
        <Text className='placeholder-title'>我的</Text>
        <Text className='placeholder-desc'>
          {isLoggedIn ? '✅ 已登录' : '⏳ 登录中...'}
        </Text>
        <Text className='placeholder-desc'>个人设置将在迭代 5 实现</Text>
      </View>
    </View>
  )
}

export default ProfilePage
