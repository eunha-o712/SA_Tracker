import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import PrivacyPolicyContent from '../PrivacyPolicy/PrivacyPolicyContent'
import './PrivacyModal.css'

function PrivacyModal({ open, onClose }) {
  const closeButtonRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined

    const previousActiveElement = document.activeElement
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    closeButtonRef.current?.focus()

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
      if (event.key === 'Tab') {
        event.preventDefault()
        closeButtonRef.current?.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
      previousActiveElement?.focus?.()
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div
      className="privacy-modal"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div
        className="privacy-modal__dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="privacy-modal-title"
      >
        <button
          ref={closeButtonRef}
          className="privacy-modal__close"
          type="button"
          aria-label="개인정보처리방침 닫기"
          onClick={onClose}
        >
          ×
        </button>
        <PrivacyPolicyContent titleId="privacy-modal-title" />
      </div>
    </div>,
    document.body
  )
}

export default PrivacyModal
