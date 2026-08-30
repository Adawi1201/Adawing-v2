// 仅接受 #rrggbb / #rgb 十六进制色值；其余值回退主题强调色
const HEX = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/

export function tagColor(color) {
  return HEX.test((color || '').trim()) ? color.trim() : 'var(--accent)'
}
