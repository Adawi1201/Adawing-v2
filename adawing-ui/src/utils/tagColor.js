// Validate a stored tag color; fall back to the theme accent for illegal
// values (e.g. legacy free-text like "blue-ish"). Only #rrggbb / #rgb pass.
const HEX = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/

export function tagColor(color) {
  return HEX.test((color || '').trim()) ? color.trim() : 'var(--accent)'
}
