/**
 * 统一导出层
 * - InkMessage: Ant Design Vue 的 message 静态 API（独立引用，体积可控）
 *   命名上叫 InkMessage 是为了避免和 Element Plus 的 ElMessage 混淆。
 *   antd v4 的 message 是命令式 API，只引入 message + rc-notification + 4 个图标，
 *   不会拉入整个组件库，gz 体积约 30-50KB。
 * - InkMessageBox: Element Plus 确认框（保持现状，其它 UI 仍用 Element Plus）
 * - InkAvatar: Element Plus 头像
 *
 * 用法（与之前完全兼容，无需改业务代码）：
 *   import { InkMessage, InkMessageBox, InkAvatar } from '@/utils/message'
 *   InkMessage.success('保存成功')
 *   InkMessage.error('出错了')
 *   InkMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
 */
import { message as AntdMessage } from 'ant-design-vue'
import { ElMessageBox as InkMessageBox, ElAvatar as InkAvatar } from 'element-plus'

// antd v4 message 签名: success(content, duration?, onClose?)，
// 同时也支持 options 对象 { content, duration, icon, class, style, onClose, ... }。
// 为了兼容 InkMessage.success(msg, options) 的旧调用，包装一层把第二个参数当成 options 合并。
function toOptions(content, options) {
  if (typeof content === 'object' && content !== null && !Array.isArray(content)) {
    // 已经是 options 形式（如 { content, duration, ... }）
    return { duration: 3, ...content, ...(options || {}) }
  }
  // 普通字符串 / VNode
  return { content, duration: 3, ...(options || {}) }
}

const InkMessage = {
  success: (content, options) => AntdMessage.success(toOptions(content, options)),
  error: (content, options) => AntdMessage.error(toOptions(content, options)),
  warning: (content, options) => AntdMessage.warning(toOptions(content, options)),
  info: (content, options) => AntdMessage.info(toOptions(content, options)),
  open: (options) => AntdMessage.open({ duration: 3, ...options }),
  config: (options) => AntdMessage.config(options),
  close: (key) => AntdMessage.destroy(key),
  destroy: () => AntdMessage.destroy(),
}

export { InkMessage, InkMessageBox, InkAvatar }
export default InkMessage
