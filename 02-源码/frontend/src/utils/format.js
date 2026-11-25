/**
 * 全局时间与数值格式化函数，全系统统一调用，禁止各页面各写一套。
 * 时间统一 yyyy-MM-dd HH:mm:ss（需求文档 227 行）。
 */
export function fmtDateTime(v) {
  if (!v) return '-'
  const d = typeof v === 'string' ? new Date(v.replace(/-/g, '/')) : new Date(v)
  if (isNaN(d.getTime())) return String(v).replace('T', ' ').slice(0, 19)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function fmtDate(v) {
  if (!v) return '-'
  return fmtDateTime(v).slice(0, 10)
}

/** 相对时间，用于消息与时间轴 */
export function fromNow(v) {
  if (!v) return '-'
  const d = new Date(String(v).replace('T', ' ').replace(/-/g, '/'))
  const diff = Date.now() - d.getTime()
  if (isNaN(diff)) return '-'
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return m + ' 分钟前'
  const h = Math.floor(m / 60)
  if (h < 24) return h + ' 小时前'
  const day = Math.floor(h / 24)
  if (day < 30) return day + ' 天前'
  return fmtDate(v)
}

export function fmtNum(v, digits = 2) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: digits })
}

/** 状态 → Element Plus tag 类型，状态不只用颜色表达，文字同步显示 */
export function statusType(status) {
  const map = {
    已完成: 'success', 已通过: 'success', 正常: 'success', 优: 'success',
    待审核: 'warning', 待验收: 'warning', 重点观察: 'warning', 中: 'warning', 良: 'primary',
    执行中: 'primary', 已提交: 'primary', 常规养护: 'primary',
    紧急抢救: 'danger', 差: 'danger', 濒危: 'danger', 已驳回: 'danger', 已销号: 'danger',
    待方案: 'info', 草稿: 'info', 暂缓: 'info', 待提交: 'info', 已迁移: 'info', 已死亡: 'danger'
  }
  return map[status] || 'info'
}
