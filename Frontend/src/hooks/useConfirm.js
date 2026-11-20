import { useContext } from 'react'
import ConfirmContext from '../components/ui/ConfirmContext'

export default function useConfirm() {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm debe usarse dentro de ConfirmProvider')
  return ctx
}
