export default function SummaryCard({ item }) {
  return (
    <article className="summary-card">
      <div className="summary-card-top">
        <span>{item.label}</span>
        <span className={`summary-card-icon ${item.color}`} aria-hidden="true">{item.icon}</span>
      </div>
      <strong className="summary-card-amount">{item.amount}</strong>
      <p>{item.note}</p>
      {item.progress ? (
        <>
          <div className="summary-progress" role="img" aria-label={`${item.progress}% of monthly budget used`}>
            <span style={{ width: `${item.progress}%` }} />
          </div>
          <small>{item.change}</small>
        </>
      ) : <small>{item.change}</small>}
    </article>
  )
}
