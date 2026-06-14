export function sourceLabel(article) {
  if (!article) return ''
  if (article.source === 'AI_GENERATED' || article.sourceAgent) return 'Agent'
  return 'Original'
}
