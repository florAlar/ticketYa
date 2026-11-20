import { useId } from 'react'
import { HiOutlineInformationCircle } from 'react-icons/hi'

/**
 * Ayuda contextual: ícono "i" enfocable que muestra una explicación al pasar el
 * mouse o al enfocar con teclado (sin dependencias).
 */
const InfoTip = ({ children, label = 'Más información' }) => {
  const id = useId()
  return (
    <span className="infotip">
      <button type="button" className="infotip__btn" aria-label={label} aria-describedby={id}>
        <HiOutlineInformationCircle aria-hidden />
      </button>
      <span role="tooltip" id={id} className="infotip__pop">
        {children}
      </span>
    </span>
  )
}

export default InfoTip
