import { Component, PropsWithChildren } from 'react'
import { login } from './service/account'
import './app.scss'

class App extends Component<PropsWithChildren> {

  // 小程序启动时静默登录
  componentDidMount() {
    login()
  }

  // this.props.children 是将要被渲染的页面
  render() {
    return this.props.children
  }
}

export default App
