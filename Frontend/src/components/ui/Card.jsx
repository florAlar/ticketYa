import { createElement } from 'react'

const Card = ({ interactive = false, as = 'div', className = '', children, ...rest }) =>
  createElement(
    as,
    {
      className: ['ui-card', interactive && 'ui-card--interactive', className]
        .filter(Boolean)
        .join(' '),
      ...rest,
    },
    children
  )

export default Card
