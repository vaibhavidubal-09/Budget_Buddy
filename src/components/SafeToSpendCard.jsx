function formatAmount(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount)
}

const statusLabels = { healthy: 'On track', caution: 'Take care', risk: 'At risk' }

export default function SafeToSpendCard({ calculation }) {
  const { safeToSpendToday, remaining, upcomingTotal, availableAfterPlans, daysRemaining, status } = calculation

  return (
    <section className={`safe-to-spend-card ${status}`} aria-labelledby="safe-to-spend-title">
      <div className="safe-to-spend-copy">
        <p className="dashboard-eyebrow">A DAILY SPENDING GUIDE</p>
        <h2 id="safe-to-spend-title">Safe to spend today</h2>
        <strong className="safe-to-spend-amount">{formatAmount(safeToSpendToday)}</strong>
        <p>Based on {formatAmount(remaining)} remaining, {formatAmount(upcomingTotal)} in planned expenses, and {daysRemaining} days left this month.</p>
      </div>
      <span className={`safe-to-spend-status ${status}`} role="status"><i aria-hidden="true" />{statusLabels[status]}</span>
      {availableAfterPlans < 0 && <p className="safe-to-spend-note">Your planned expenses are more than your current remaining balance.</p>}
    </section>
  )
}
