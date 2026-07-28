// Single source of truth for Vditor Markdown rendering options.
//
// Both the admin editor (`new Vditor(el, { preview })` in ArticleEditorView) and
// the visitor/shared renderer (`Vditor.preview(el, md, opts)` in MarkdownContent)
// draw their render config from here, so editing preview and visitor display stay
// identical. Assets are self-hosted under /vditor (see vite.config.js), so KaTeX /
// highlight.js / lute load from our own server instead of the external CDN.

export const VDITOR_CDN = '/vditor'

// Render keys shared by IPreviewOptions (Vditor.preview) and IPreview
// (the editor's `preview` sub-object). Same semantics on both sides.
export function renderOptions() {
  return {
    hljs: { style: 'github', enable: true, lineNumber: false },
    math: { engine: 'KaTeX', inlineDigit: true },
    markdown: { toc: true, mark: true }
  }
}

// Options for `Vditor.preview(el, md, options)` — used by MarkdownContent.
export function buildPreviewOptions(isDark) {
  return {
    mode: isDark ? 'dark' : 'light',
    cdn: VDITOR_CDN,
    ...renderOptions(),
    theme: { current: isDark ? 'dark' : 'light' }
  }
}

// The `preview` sub-object for `new Vditor(el, { preview })` — used by the editor.
export function buildEditorPreview(isDark) {
  return {
    ...renderOptions(),
    theme: { current: isDark ? 'dark' : 'light' }
  }
}
