// Small wrapper around browser storage so page/service code stays easy to read.
export function readStoredValue(key, fallbackValue) {
  try {
    const savedValue = localStorage.getItem(key)
    return savedValue ? JSON.parse(savedValue) : fallbackValue
  } catch {
    return fallbackValue
  }
}

export function saveStoredValue(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // The app can still run in memory if browser storage is unavailable.
  }
}
