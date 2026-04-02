module.exports = {
  presets: [
    [
      'taro',
      {
        framework: 'react',
        ts: true,
        compiler: 'webpack5',
      },
    ],
  ],
  // 显式 transform ?.  ??  catch{} 语法
  // package.json 的 browserslist 目标是 Chrome/Firefox，@babel/preset-env 不会降级这些语法。
  // 但微信小程序 JS 引擎版本不统一，显式插件确保无论目标环境如何都会降级，避免真机报错。
  plugins: [
    '@babel/plugin-transform-optional-chaining',
    '@babel/plugin-transform-nullish-coalescing-operator',
    '@babel/plugin-transform-optional-catch-binding',
  ],
}
