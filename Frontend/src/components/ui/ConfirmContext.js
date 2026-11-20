import { createContext } from 'react'

/** confirm({ title, description, confirmLabel, tone }) → Promise<boolean> */
const ConfirmContext = createContext(null)
export default ConfirmContext
