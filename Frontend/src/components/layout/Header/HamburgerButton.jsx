import { HiOutlineMenuAlt3 } from 'react-icons/hi'

const HamburgerButton = ({ toggle }) => {
  return (
    <button
      className="menu-toggle"
      onClick={toggle}
      aria-label="Open menu"
    >
      <HiOutlineMenuAlt3 />
    </button>
  )
}

export default HamburgerButton