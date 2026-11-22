export const defaultSettings = {
  budgetAlerts: true,
  weeklySummary: false,
  spendingTips: true,
}

export function getSavedSettings() {
  try {
    const saved = localStorage.getItem('budgetbuddy-settings')
    return saved ? { ...defaultSettings, ...JSON.parse(saved) } : defaultSettings
  } catch {
    return defaultSettings
  }
}
