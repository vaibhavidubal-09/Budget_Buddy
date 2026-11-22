const DAY_MS = 24 * 60 * 60 * 1000

function parseDate(value) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function formatDate(date) {
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`
}

function clampDay(year, monthIndex, day) {
  return Math.min(day, new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate())
}

export function getIncomeOccurrences(entry, start, end) {
  const firstDate = parseDate(entry.date)
  const rangeStart = parseDate(start)
  const rangeEnd = parseDate(end)
  const dates = []
  const frequency = entry.frequency || 'ONCE'

  if (frequency === 'ONCE') {
    return firstDate >= rangeStart && firstDate <= rangeEnd ? [entry.date] : []
  }

  if (frequency === 'WEEKLY' || frequency === 'CUSTOM') {
    const interval = frequency === 'WEEKLY' ? 7 : Math.max(1, Number(entry.intervalDays) || 1)
    const daysUntilRange = Math.ceil((rangeStart - firstDate) / DAY_MS)
    const firstIndex = Math.max(0, Math.ceil(daysUntilRange / interval))
    let occurrence = new Date(firstDate.getTime() + firstIndex * interval * DAY_MS)
    while (occurrence <= rangeEnd) {
      dates.push(formatDate(occurrence))
      occurrence = new Date(occurrence.getTime() + interval * DAY_MS)
    }
    return dates
  }

  const anchorDay = firstDate.getUTCDate()
  if (frequency === 'MONTHLY') {
    let year = Math.max(firstDate.getUTCFullYear(), rangeStart.getUTCFullYear())
    let monthIndex = year === firstDate.getUTCFullYear() ? firstDate.getUTCMonth() : 0
    while (year < rangeEnd.getUTCFullYear() || (year === rangeEnd.getUTCFullYear() && monthIndex <= rangeEnd.getUTCMonth())) {
      const occurrence = new Date(Date.UTC(year, monthIndex, clampDay(year, monthIndex, anchorDay)))
      if (occurrence >= firstDate && occurrence >= rangeStart && occurrence <= rangeEnd) dates.push(formatDate(occurrence))
      monthIndex += 1
      if (monthIndex === 12) { monthIndex = 0; year += 1 }
    }
    return dates
  }

  if (frequency === 'YEARLY') {
    const monthIndex = firstDate.getUTCMonth()
    for (let year = Math.max(firstDate.getUTCFullYear(), rangeStart.getUTCFullYear()); year <= rangeEnd.getUTCFullYear(); year += 1) {
      const occurrence = new Date(Date.UTC(year, monthIndex, clampDay(year, monthIndex, anchorDay)))
      if (occurrence >= firstDate && occurrence >= rangeStart && occurrence <= rangeEnd) dates.push(formatDate(occurrence))
    }
  }
  return dates
}

export function getIncomeTotalInRange(income, start, end) {
  return income.reduce((total, entry) => total + getIncomeOccurrences(entry, start, end).length * Number(entry.amount), 0)
}

// Convert repeating payments to an equivalent amount for the selected budget period.
// A yearly payment should contribute 1/52 of its value to a weekly plan, not the
// entire amount just because its payday happened during that week.
export function getExpectedIncomeForPeriod(income, period, start, end) {
  const periodDays = period === 'weekly' ? 7 : period === 'monthly' ? 365 / 12 : 365
  return income.reduce((total, entry) => {
    const amount = Number(entry.amount) || 0
    const frequency = entry.frequency || 'ONCE'
    if (frequency === 'ONCE') {
      return total + getIncomeOccurrences(entry, start, end).length * amount
    }

    // A repeating source is not expected before its first scheduled payment.
    if (entry.date > end) return total

    let periodPayments
    if (frequency === 'WEEKLY') periodPayments = periodDays / 7
    else if (frequency === 'MONTHLY') periodPayments = periodDays * 12 / 365
    else if (frequency === 'YEARLY') periodPayments = periodDays / 365
    else if (frequency === 'CUSTOM') periodPayments = periodDays / Math.max(1, Number(entry.intervalDays) || 1)
    else return total

    return total + amount * periodPayments
  }, 0)
}

export function getIncomeTotalForMonth(income, today = new Date()) {
  const year = today.getFullYear()
  const month = today.getMonth()
  const start = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const lastDay = new Date(year, month + 1, 0).getDate()
  const end = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  return getIncomeTotalInRange(income, start, end)
}

export function getIncomeFrequencyLabel(entry) {
  switch (entry.frequency || 'ONCE') {
    case 'WEEKLY': return 'Weekly'
    case 'MONTHLY': return 'Monthly'
    case 'YEARLY': return 'Yearly'
    case 'CUSTOM': return `Every ${entry.intervalDays || 1} days`
    default: return 'One time'
  }
}
