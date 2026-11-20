import { Toaster } from 'sonner'
import useTheme from '../../hooks/useTheme'

/** Toasts globales (sonner) alineados con el tema claro/oscuro. */
const AppToaster = () => {
  const { themeName } = useTheme()
  return (
    <Toaster
      theme={themeName === 'dark' ? 'dark' : 'light'}
      position="bottom-right"
      richColors
      closeButton
      toastOptions={{ style: { fontFamily: 'var(--fonts-body)', borderRadius: '14px' } }}
    />
  )
}

export default AppToaster
