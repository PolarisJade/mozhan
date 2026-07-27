import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import './style.css'
import './styles/ink-theme.css'
// 注意：消息提示改用 antd message，样式由 antd 的 CSS-in-JS 自动注入，无需额外 css

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
