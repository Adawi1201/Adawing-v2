// ── Bubble toast notification ──
// Singleton floating notification, no alert() pollution.

let timer = null

export function toast(text, type = 'info') {
  if (typeof document === 'undefined') return

  // remove existing toast
  const prev = document.querySelector('.bubble-toast')
  if (prev) prev.remove()
  clearTimeout(timer)

  const el = document.createElement('div')
  el.className = `bubble-toast bubble-toast-${type}`
  el.textContent = text
  document.body.appendChild(el)

  requestAnimationFrame(() => el.classList.add('bubble-toast-in'))

  timer = setTimeout(() => {
    el.classList.remove('bubble-toast-in')
    setTimeout(() => el.remove(), 280)
  }, 3500)
}

// Inject styles once
if (typeof document !== 'undefined' && !document.querySelector('#bubble-toast-style')) {
  const style = document.createElement('style')
  style.id = 'bubble-toast-style'
  style.textContent = `
.bubble-toast {
  position: fixed; bottom: 28px; right: 28px; z-index: 99999;
  max-width: 400px; padding: 12px 20px;
  font-size: 13px; font-family: inherit;
  border: 1px solid var(--line, rgba(0,0,0,.18));
  background: var(--bg, #FEFEFE); color: var(--ink, #2D2D2D);
  letter-spacing: 0.03em; border-radius: 6px;
  box-shadow: 0 6px 24px rgba(0,0,0,.14);
  opacity: 0; transform: translateY(10px) scale(0.98);
  transition: opacity 0.22s ease, transform 0.22s ease;
  pointer-events: none;
  line-height: 1.45;
}
.bubble-toast-in {
  opacity: 1; transform: translateY(0) scale(1);
}
.bubble-toast-error {
  border-color: #d47272; color: #b84c4c;
  background: rgba(196,92,92,.07);
}
.bubble-toast-warn {
  border-color: #c9a44a; color: #8b6914;
  background: rgba(217,175,62,.08);
}
.bubble-toast-success {
  border-color: #5ea87a; color: #3a6b4e;
  background: rgba(94,168,122,.07);
}
`
  document.head.appendChild(style)
}
