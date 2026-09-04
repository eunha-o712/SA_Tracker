import DOMPurify from 'dompurify'

const NOTICE_TAGS = [
  'section', 'header', 'article', 'div',
  'h2', 'h3', 'p', 'ol', 'ul', 'li',
  'strong', 'span', 'small', 'br',
]

const NOTICE_ATTRIBUTES = ['class', 'aria-label', 'aria-hidden']

export function sanitizeNoticeHtml(html) {
  return DOMPurify.sanitize(html ?? '', {
    ALLOWED_TAGS: NOTICE_TAGS,
    ALLOWED_ATTR: NOTICE_ATTRIBUTES,
  })
}

export function isRichNoticeHtml(html) {
  return /<section\s+[^>]*class=["'][^"']*\bnotice-guide\b/i.test(html ?? '')
}
