export const expenseCategories = [
  'Food & Dining',
  'Transportation',
  'Shopping',
  'Education',
  'Entertainment',
  'Bills & Subscriptions',
  'Healthcare',
  'Other',
]

export const paymentMethods = ['UPI', 'Cash', 'Card', 'Other']

// These rows are sample expenses. The page edits them in React state only.
export const initialExpenses = [
  { id: 1, description: 'Lunch at campus cafe', category: 'Food & Dining', amount: 160, paymentMethod: 'UPI', date: '2026-10-07' },
  { id: 2, description: 'Auto ride home', category: 'Transportation', amount: 120, paymentMethod: 'UPI', date: '2026-10-06' },
  { id: 3, description: 'Course notebook and pens', category: 'Education', amount: 285, paymentMethod: 'Cash', date: '2026-10-05' },
  { id: 4, description: 'Coffee with friends', category: 'Food & Dining', amount: 210, paymentMethod: 'UPI', date: '2026-10-04' },
  { id: 5, description: 'Monthly music plan', category: 'Bills & Subscriptions', amount: 119, paymentMethod: 'Card', date: '2026-10-03' },
  { id: 6, description: 'Metro card top-up', category: 'Transportation', amount: 250, paymentMethod: 'UPI', date: '2026-10-02' },
  { id: 7, description: 'Movie night', category: 'Entertainment', amount: 320, paymentMethod: 'Card', date: '2026-10-01' },
]
