import { HiOutlineMoon, HiOutlineSun } from 'react-icons/hi'
import useTheme from '../../../hooks/useTheme'

/** Alterna claro/oscuro (persistido en localStorage por ThemeProvider). */
const ThemeToggle = () => {
  const { themeName, toggleTheme } = useTheme()
  const dark = themeName === 'dark'
  const label = dark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'
  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggleTheme}
      aria-label={label}
      title={label}
    >
      {dark ? <HiOutlineSun aria-hidden /> : <HiOutlineMoon aria-hidden />}
    </button>
  )
}

export default ThemeToggle
