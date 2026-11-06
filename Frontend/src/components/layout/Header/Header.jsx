import { useEffect, useState } from 'react'

import Logo from './Logo'
import DesktopNav from './DesktopNav'
import HamburgerButton from './HamburgerButton'
import MobileMenu from './MobileMenu'
import SearchBar from './SearchBar'
import '../../../styles/global.css'

const SCROLL_THRESHOLD = 50

const Header = () => {
  const [isScrolled, setIsScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > SCROLL_THRESHOLD)
    }
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const toggleMenu = () => setMenuOpen(v => !v)
  const closeMenu = () => setMenuOpen(false)

  return (
    <header className={`site-header ${isScrolled ? 'scrolled' : ''}`}>
      <div className="header-inner">
        <Logo onClick={closeMenu} />

        <div className="header-right">
          <SearchBar />
          <DesktopNav />
          <HamburgerButton toggle={toggleMenu} />
        </div>
      </div>

      <MobileMenu open={menuOpen} close={closeMenu} />
    </header>
  )
}

export default Header
