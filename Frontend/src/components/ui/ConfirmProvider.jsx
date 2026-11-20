import { useCallback, useRef, useState } from 'react'
import { HiOutlineExclamation } from 'react-icons/hi'
import ConfirmContext from './ConfirmContext'
import Modal from './Modal'
import Button from './Button'

/** Reemplaza window.confirm con un modal del sistema de diseño. */
const ConfirmProvider = ({ children }) => {
  const [req, setReq] = useState(null)
  const resolver = useRef(null)

  const confirm = useCallback(
    (opts) =>
      new Promise((resolve) => {
        resolver.current = resolve
        setReq(typeof opts === 'string' ? { title: opts } : opts)
      }),
    []
  )

  const settle = (value) => {
    resolver.current?.(value)
    resolver.current = null
    setReq(null)
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {req && (
        <Modal
          icon={HiOutlineExclamation}
          title={req.title}
          description={req.description}
          onClose={() => settle(false)}
          actions={
            <>
              <Button variant="ghost" onClick={() => settle(false)}>
                {req.cancelLabel || 'Cancelar'}
              </Button>
              <Button
                variant={req.tone === 'danger' ? 'danger' : 'primary'}
                onClick={() => settle(true)}
              >
                {req.confirmLabel || 'Confirmar'}
              </Button>
            </>
          }
        />
      )}
    </ConfirmContext.Provider>
  )
}

export default ConfirmProvider
