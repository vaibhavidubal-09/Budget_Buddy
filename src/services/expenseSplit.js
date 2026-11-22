// Keep split arithmetic in integer paise so the shares always reconcile to the bill.
export function calculateSplitShares(totalAmount, participants, mode = 'equal') {
  const totalPaise = Math.round(Number(totalAmount || 0) * 100)
  if (!Array.isArray(participants) || participants.length === 0) return []

  if (mode === 'custom') {
    return participants.map(person => Math.round(Number(person.share || 0) * 100))
  }

  const baseShare = Math.floor(totalPaise / participants.length)
  const remainingPaise = totalPaise % participants.length
  return participants.map((_, index) => baseShare + (index < remainingPaise ? 1 : 0))
}

export function sharesMatchTotal(totalAmount, sharesInPaise) {
  const totalPaise = Math.round(Number(totalAmount || 0) * 100)
  return sharesInPaise.length > 0
    && sharesInPaise.every(share => Number.isInteger(share) && share >= 0)
    && sharesInPaise.reduce((sum, share) => sum + share, 0) === totalPaise
}
