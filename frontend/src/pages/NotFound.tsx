import React from 'react'

function NotFound() {
  return (
    <>
      <h1 className="govuk-heading-xl">Page not found</h1>
      <p className="govuk-body">
        If you typed the web address, check it is correct.
      </p>
      <p className="govuk-body">
        <a href="/" className="govuk-link">Go to the dashboard</a>
      </p>
    </>
  )
}

export default NotFound
