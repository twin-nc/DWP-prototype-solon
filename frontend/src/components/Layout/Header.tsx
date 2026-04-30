import React from 'react'

function Header() {
  return (
    <header className="govuk-header" role="banner" data-module="govuk-header">
      <div className="govuk-header__container govuk-width-container">
        <div className="govuk-header__logo">
          <a href="/" className="govuk-header__link govuk-header__link--homepage">
            <span className="govuk-header__logotype">
              <span className="govuk-header__logotype-text">GOV.UK</span>
            </span>
          </a>
        </div>
        <div className="govuk-header__content">
          <a href="/" className="govuk-header__link govuk-header__link--service-name">
            Debt Collection Management System
          </a>
        </div>
      </div>
    </header>
  )
}

export default Header
