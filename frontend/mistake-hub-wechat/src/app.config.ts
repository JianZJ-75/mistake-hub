export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/mistake/index',
    'pages/review/index',
    'pages/profile/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#4A7CF8',
    navigationBarTitleText: '错题复习',
    navigationBarTextStyle: 'white',
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#4A7CF8',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页',
        iconPath: 'assets/tabbar/home.png',
        selectedIconPath: 'assets/tabbar/home-active.png',
      },
      {
        pagePath: 'pages/mistake/index',
        text: '错题',
        iconPath: 'assets/tabbar/mistake.png',
        selectedIconPath: 'assets/tabbar/mistake-active.png',
      },
      {
        pagePath: 'pages/review/index',
        text: '复习',
        iconPath: 'assets/tabbar/review.png',
        selectedIconPath: 'assets/tabbar/review-active.png',
      },
      {
        pagePath: 'pages/profile/index',
        text: '我的',
        iconPath: 'assets/tabbar/profile.png',
        selectedIconPath: 'assets/tabbar/profile-active.png',
      },
    ],
  },
})
