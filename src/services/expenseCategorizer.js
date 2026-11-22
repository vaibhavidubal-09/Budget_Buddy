// A small frontend demo: keywords suggest a category until a real AI service exists.
const categoryKeywords = {
  'Food & Dining': ['food', 'lunch', 'dinner', 'breakfast', 'cafe', 'coffee', 'restaurant', 'pizza', 'snack', 'grocery', 'groceries', 'swiggy', 'zomato'],
  Transportation: ['travel', 'transport', 'auto', 'uber', 'ola', 'taxi', 'bus', 'metro', 'train', 'fuel', 'petrol', 'parking', 'college'],
  Shopping: ['shopping', 'amazon', 'clothes', 'shirt', 'shoes', 'flipkart', 'store'],
  Education: ['book', 'notebook', 'course', 'college', 'tuition', 'exam', 'stationery', 'pen'],
  Entertainment: ['movie', 'cinema', 'game', 'concert', 'netflix', 'spotify'],
  'Bills & Subscriptions': ['bill', 'electricity', 'internet', 'phone', 'subscription', 'recharge'],
}

export function suggestExpenseCategory(description) {
  const words = description.toLowerCase().match(/[a-z0-9]+/g) || []

  for (const [category, keywords] of Object.entries(categoryKeywords)) {
    if (words.some(word => keywords.includes(word))) return category
  }

  return description.trim() ? 'Other' : null
}
