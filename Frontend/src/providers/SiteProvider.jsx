import SiteContext from '../context/SiteContext'
import site from '../config/site'

/** Brand / copy de chrome (logo, footer). No mezclar con copy de plantilla. */
export default function SiteProvider({ children }) {
  return (
    <SiteContext.Provider value={{ siteInfo: site }}>
      {children}
    </SiteContext.Provider>
  )
}
