import { initialProfile } from '../data/profileData.js'
import { readStoredValue, saveStoredValue } from './browserStorage.js'

export function getProfile() {
  return readStoredValue('budgetbuddy-profile', initialProfile)
}

export function saveProfile(profile) {
  saveStoredValue('budgetbuddy-profile', profile)
}
