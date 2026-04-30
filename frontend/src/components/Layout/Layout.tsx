import React from 'react'
import { Outlet } from 'react-router-dom'
import Header from './Header'
import Footer from './Footer'

function Layout() {
  return (
    <>
      <Header />
      <div className="govuk-width-container">
        <main className="govuk-main-wrapper" id="main-content" role="main">
          <Outlet />
        </main>
      </div>
      <Footer />
    </>
  )
}

export default Layout
