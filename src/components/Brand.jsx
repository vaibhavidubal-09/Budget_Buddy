import { Link } from 'react-router-dom'

export default function Brand() {
  return (
    <Link className="brand" to="/" aria-label="BudgetBuddy home">
      <span className="brand-mark" aria-hidden="true">{String.fromCodePoint(8377)}</span>
      <span>Budget<span className="brand-buddy">Buddy</span></span>
    </Link>
  )
}
