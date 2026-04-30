import { render } from '@testing-library/react'
import { axe, toHaveNoViolations } from 'vitest-axe'
import { expect, it, describe } from 'vitest'
import App from '../App'

expect.extend(toHaveNoViolations)

describe('accessibility — critical violations', () => {
  it('App renders with no critical axe violations', async () => {
    const { container } = render(<App />)
    const results = await axe(container, {
      runOnly: {
        type: 'tag',
        values: ['wcag2a', 'wcag2aa'],
      },
    })
    expect(results).toHaveNoViolations()
  })
})
