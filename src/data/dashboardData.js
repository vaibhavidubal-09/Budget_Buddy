const rupee = String.fromCodePoint(8377)

// Keeping sample values together makes it easier to update the dashboard later.
export const summaryCards = [
  {
    label: 'Monthly income',
    amount: `${rupee}5,000`,
    note: 'Your allowance this month',
    change: 'Received on Oct 1',
    icon: String.fromCodePoint(8595),
    color: 'mint',
  },
  {
    label: 'Total expenses',
    amount: `${rupee}3,240`,
    note: 'Across 28 purchases',
    change: 'Food is your top spend',
    icon: String.fromCodePoint(8599),
    color: 'peach',
  },
  {
    label: 'Current balance',
    amount: `${rupee}1,760`,
    note: 'Money left for this month',
    change: '35% of your allowance',
    icon: rupee,
    color: 'lilac',
  },
  {
    label: 'Monthly budget',
    amount: `${rupee}4,000`,
    note: `${rupee}760 still available`,
    change: '81% used',
    icon: String.fromCodePoint(10033),
    color: 'yellow',
    progress: 81,
  },
]
