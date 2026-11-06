import Header from './Header/Header'
import Footer from './Footer/Footer'
import '../../styles/global.css'

const Layout = ({ children }) => {
  return (
    <div className="layout-wrapper">
      <Header />
      <main className="main-content">{children}</main>
      <Footer />
    </div>
  )
}

export default Layout
