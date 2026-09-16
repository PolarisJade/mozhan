import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 顺序不能动：ink-theme.css 覆盖的是 --el-* 变量，必须在 Element Plus 之后；
// style.css 是基础重置，放在主题之前，让主题的纸张色和字族盖住它。
import './style.css'
import './styles/ink-theme.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const app = createApp(App)

app.use(router)
app.use(ElementPlus, {
    locale: zhCn
})

app.mount('#app')
