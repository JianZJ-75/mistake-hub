import { Component, PropsWithChildren } from 'react'
import { login } from './service/account'
import './app.scss'

class App extends Component<PropsWithChildren> {

  constructor(props: PropsWithChildren) {
    super(props)
    // 在 constructor 中调用 login()，确保 _pendingLogin 在子页面渲染前已注册
    // 子页面发起请求时若无 token，会自动等待此 promise 完成后再发出
    login()
  }

  // this.props.children 是将要被渲染的页面
  render() {
    return this.props.children
  }
}

export default App
