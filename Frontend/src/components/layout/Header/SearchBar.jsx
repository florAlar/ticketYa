import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { HiOutlineSearch } from 'react-icons/hi'

/** Barra de búsqueda → /eventos?q=… (Enter; lupa decorativa) */
const SearchBar = () => {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [q, setQ] = useState(params.get('q') || '')

  const onSubmit = (e) => {
    e.preventDefault()
    const value = q.trim()
    const next = value ? `/eventos?q=${encodeURIComponent(value)}` : '/eventos'
    navigate(next)
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
