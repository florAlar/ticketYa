import { useNavigate, useSearchParams } from 'react-router-dom'
import { HiOutlineSearch } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'

/**
 * Barra de búsqueda por palabra → /eventos?q=…
 * Catálogo es JWT: sin sesión manda a login con ?next=.
 * No hay link «Eventos» en el nav; la búsqueda es el acceso.
 *
 * Input no controlado: la URL es la única fuente de verdad. `key` remonta el
 * campo cuando cambia `q` (atrás/adelante), sin efecto de sincronización.
 */
const SearchBar = () => {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { isAuthenticated } = useAuth()
  const currentQuery = params.get('q') || ''

  const onSubmit = (e) => {
    e.preventDefault()
    const value = String(new FormData(e.currentTarget).get('q') || '').trim()
    const catalog = value
      ? `/eventos?q=${encodeURIComponent(value)}`
      : '/eventos'
    if (!isAuthenticated) {
      navigate(`/login?next=${encodeURIComponent(catalog)}`)
      return
    }
    navigate(catalog)
  }

  return (
    <form className="header-search" onSubmit={onSubmit} role="search">
      <HiOutlineSearch className="header-search__icon" aria-hidden />
      <input
        key={currentQuery}
        type="search"
        name="q"
        defaultValue={currentQuery}
        placeholder="Buscar eventos"
        aria-label="Buscar eventos"
        enterKeyHint="search"
      />
    </form>
  )
}

export default SearchBar
