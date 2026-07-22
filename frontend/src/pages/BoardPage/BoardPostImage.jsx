import { useEffect, useState } from 'react'
import api from '../../api/api'

function BoardPostImage({ imageUrl, alt, privatePost = false }) {
  if (!privatePost) {
    return <img src={resolveBoardImageUrl(imageUrl)} alt={alt} loading="lazy" />
  }

  return <PrivateBoardPostImage imageUrl={imageUrl} alt={alt} />
}

function PrivateBoardPostImage({ imageUrl, alt }) {
  const [loaded, setLoaded] = useState({ imageUrl: '', source: '', failed: false })

  useEffect(() => {
    let active = true
    let objectUrl = ''

    api.get(imageUrl, { responseType: 'blob' })
      .then(({ data }) => {
        if (!active) return
        objectUrl = URL.createObjectURL(data)
        setLoaded({ imageUrl, source: objectUrl, failed: false })
      })
      .catch(() => {
        if (active) setLoaded({ imageUrl, source: '', failed: true })
      })

    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [imageUrl])

  if (loaded.imageUrl === imageUrl && loaded.failed) {
    return <span className="board-private-image-state">이미지를 불러올 수 없습니다.</span>
  }
  if (loaded.imageUrl !== imageUrl || !loaded.source) {
    return <span className="board-private-image-state" aria-busy="true">보호된 이미지 불러오는 중</span>
  }
  return <img src={loaded.source} alt={alt} loading="lazy" />
}

function resolveBoardImageUrl(value) {
  if (/^https?:\/\//i.test(value)) return value
  return `${String(api.defaults.baseURL ?? '').replace(/\/$/, '')}${value}`
}

export default BoardPostImage
