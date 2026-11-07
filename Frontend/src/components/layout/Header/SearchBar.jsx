import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { HiOutlineSearch } from 'react-icons/hi'
import useAuth from '../../../hooks/useAuth'

/**
 * Barra de búsqueda por palabra → /eventos?q=…
 * Catálogo es JWT: sin sesión manda a login con ?next=.
 * No hay link «Eventos» en el nav; la búsqueda es el acceso.
 */
const SearchBar = () => {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { isAuthenticated } = useAuth()
  const [q, setQ] = useState(params.get('q') || '')

  useEffect(() => {
    setQ(params.get('q') || '')
  }, [params])

  const onSubmit = (e) => {
    e.preventDefault()
    const value = q.trim()
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
        type="search"
        name="q"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Buscar eventos"
        aria-label="Buscar eventos"
        enterKeyHint="search"
      />
    </form>
  )
}

export default SearchBar
