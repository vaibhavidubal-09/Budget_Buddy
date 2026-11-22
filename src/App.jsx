import Landing from './pages/Landing.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import ExpensesPage from './pages/ExpensesPage.jsx'
import IncomePage from './pages/IncomePage.jsx'
import BudgetsPage from './pages/BudgetsPage.jsx'
import AnalyticsPage from './pages/AnalyticsPage.jsx'
import AssistantPage from './pages/AssistantPage.jsx'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { importExpensesFromApi, loadExpensesFromApi, syncExpenseChanges } from './services/expenseApiService.js'
import { loadIncomeFromApi, syncIncomeChanges } from './services/incomeApiService.js'
import { applyBudgetPlanFromApi, getBudgetsFromApi, loadBudgetsFromApi, syncBudgetChanges } from './services/budgetApiService.js'
import ProfilePage from './pages/ProfilePage.jsx'
import SettingsPage from './pages/SettingsPage.jsx'
import { getSavedSettings } from './data/settingsData.js'
import { getProfile, saveProfile } from './services/profileService.js'
import { getMonthlySummaryFromApi } from './services/summaryApiService.js'
import { clearAuthSession, getSignedInUser } from './services/authApiService.js'

function ProtectedPage({ authUser, children }) {
  return authUser ? children : <Navigate to="/login" replace />
}

export default function App() {
  const [theme, setTheme] = useState(() => localStorage.getItem('budgetbuddy-theme') || 'midnight')
  const [authUser, setAuthUser] = useState(getSignedInUser)
  const [expenses, setExpenses] = useState([])
  const [expenseApiStatus, setExpenseApiStatus] = useState('checking')
  const [income, setIncome] = useState([])
  const [incomeApiStatus, setIncomeApiStatus] = useState('checking')
  const [budgets, setBudgets] = useState([])
  const [budgetApiStatus, setBudgetApiStatus] = useState('checking')
  const [monthlySummary, setMonthlySummary] = useState(null)
  const [summaryApiStatus, setSummaryApiStatus] = useState('checking')
  const [profile, setProfile] = useState(getProfile)
  const [settings, setSettings] = useState(getSavedSettings)

  useEffect(() => {
    localStorage.setItem('budgetbuddy-theme', theme)
  }, [theme])

  useEffect(() => {
    localStorage.setItem('budgetbuddy-settings', JSON.stringify(settings))
  }, [settings])

  useEffect(() => {
    if (!authUser) {
      setExpenses([])
      setExpenseApiStatus('offline')
      return
    }
    let ignoreResponse = false

    async function fetchExpensesOnStartup() {
      try {
        const apiExpenses = await loadExpensesFromApi()

        if (!ignoreResponse) {
          setExpenses(apiExpenses)
          setExpenseApiStatus('connected')
        }
      } catch {
        if (!ignoreResponse) setExpenseApiStatus('offline')
      }
    }

    fetchExpensesOnStartup()
    return () => { ignoreResponse = true }
  }, [authUser?.id])

  useEffect(() => {
    if (!authUser) {
      setIncome([])
      setIncomeApiStatus('offline')
      return
    }
    let ignoreResponse = false

    async function fetchIncomeOnStartup() {
      try {
        const apiIncome = await loadIncomeFromApi()

        if (!ignoreResponse) {
          setIncome(apiIncome)
          setIncomeApiStatus('connected')
        }
      } catch {
        if (!ignoreResponse) setIncomeApiStatus('offline')
      }
    }

    fetchIncomeOnStartup()
    return () => { ignoreResponse = true }
  }, [authUser?.id])

  useEffect(() => {
    if (!authUser) {
      setBudgets([])
      setBudgetApiStatus('offline')
      return
    }
    let ignoreResponse = false

    async function fetchBudgetsOnStartup() {
      try {
        const apiBudgets = await loadBudgetsFromApi()
        if (!ignoreResponse) {
          setBudgets(apiBudgets)
          setBudgetApiStatus('connected')
        }
      } catch {
        if (!ignoreResponse) setBudgetApiStatus('offline')
      }
    }

    fetchBudgetsOnStartup()
    return () => { ignoreResponse = true }
  }, [authUser?.id])

  useEffect(() => {
    saveProfile(profile)
  }, [profile])

  useEffect(() => {
    let ignoreResponse = false

    if (!authUser) {
      setMonthlySummary(null)
      setSummaryApiStatus('offline')
      return
    }

    getMonthlySummaryFromApi()
      .then(summary => {
        if (!ignoreResponse) {
          setMonthlySummary(summary)
          setSummaryApiStatus('connected')
        }
      })
      .catch(() => {
        if (!ignoreResponse) setSummaryApiStatus('offline')
      })

    return () => { ignoreResponse = true }
  }, [authUser?.id, expenses, income, budgets])

  function handleAuthenticated(user) {
    setExpenses([])
    setIncome([])
    setBudgets([])
    setExpenseApiStatus('checking')
    setIncomeApiStatus('checking')
    setBudgetApiStatus('checking')
    setSummaryApiStatus('checking')
    setAuthUser(user)
    setProfile(current => ({ ...current, fullName: user.fullName, email: user.email }))
  }

  function handleLogout() {
    clearAuthSession()
    setAuthUser(null)
    setExpenses([])
    setIncome([])
    setBudgets([])
    setMonthlySummary(null)
  }

  async function handleExpensesChange(nextExpenses) {
    if (expenseApiStatus !== 'connected') return false
    const previousExpenses = expenses
    setExpenses(nextExpenses)

    try {
      const savedExpenses = await syncExpenseChanges(expenses, nextExpenses)
      setExpenses(savedExpenses)
      return true
    } catch {
      setExpenses(previousExpenses)
      setExpenseApiStatus('offline')
      return false
    }
  }

  async function handleExpenseImport(transactions) {
    if (expenseApiStatus !== 'connected') return null
    try {
      const result = await importExpensesFromApi(transactions)
      setExpenses(await loadExpensesFromApi())
      return result
    } catch {
      setExpenseApiStatus('offline')
      return null
    }
  }

  async function handleIncomeChange(nextIncome) {
    if (incomeApiStatus !== 'connected') return false
    const previousIncome = income
    setIncome(nextIncome)

    try {
      const savedIncome = await syncIncomeChanges(income, nextIncome)
      setIncome(savedIncome)
      return true
    } catch {
      setIncome(previousIncome)
      setIncomeApiStatus('offline')
      return false
    }
  }

  async function handleBudgetsChange(nextBudgets) {
    if (budgetApiStatus !== 'connected') return false
    const previousBudgets = budgets
    setBudgets(nextBudgets)

    try {
      const savedBudgets = await syncBudgetChanges(budgets, nextBudgets)
      setBudgets(savedBudgets)
      return true
    } catch {
      setBudgets(previousBudgets)
      setBudgetApiStatus('offline')
      return false
    }
  }

  async function handleBudgetPlanApply(plan) {
    if (budgetApiStatus !== 'connected') return { success: false, error: 'Backend unavailable. Reconnect and try again.' }
    try {
      const savedBudgets = await applyBudgetPlanFromApi(plan)
      setBudgets(savedBudgets)
      return { success: true }
    } catch (error) {
      try {
        setBudgets(await getBudgetsFromApi())
      } catch {
        setBudgetApiStatus('offline')
      }
      return { success: false, error: error.message || 'Could not apply the budget plan. The latest saved budgets have been reloaded.' }
    }
  }

  return (
    <div className="app-root" data-theme={theme}>
      <Routes>
        <Route path="/" element={<Landing theme={theme} onThemeChange={setTheme} />} />
        <Route path="/login" element={authUser ? <Navigate to="/dashboard" replace /> : <LoginPage theme={theme} onThemeChange={setTheme} onAuthenticated={handleAuthenticated} />} />
        <Route path="/register" element={authUser ? <Navigate to="/dashboard" replace /> : <RegisterPage theme={theme} onThemeChange={setTheme} onAuthenticated={handleAuthenticated} />} />
        <Route path="/dashboard" element={<ProtectedPage authUser={authUser}><DashboardPage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} expenses={expenses} income={income} budgets={budgets} monthlySummary={monthlySummary} summaryApiStatus={summaryApiStatus} profile={profile} /></ProtectedPage>} />
        <Route path="/expenses" element={<ProtectedPage authUser={authUser}><ExpensesPage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} expenses={expenses} onExpensesChange={handleExpensesChange} onImportExpenses={handleExpenseImport} apiStatus={expenseApiStatus} profile={profile} /></ProtectedPage>} />
        <Route path="/income" element={<ProtectedPage authUser={authUser}><IncomePage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} profile={profile} income={income} onIncomeChange={handleIncomeChange} apiStatus={incomeApiStatus} /></ProtectedPage>} />
        <Route path="/budgets" element={<ProtectedPage authUser={authUser}><BudgetsPage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} profile={profile} budgets={budgets} onBudgetsChange={handleBudgetsChange} onBudgetPlanApply={handleBudgetPlanApply} expenses={expenses} income={income} apiStatus={budgetApiStatus} /></ProtectedPage>} />
        <Route path="/analytics" element={<ProtectedPage authUser={authUser}><AnalyticsPage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} profile={profile} expenses={expenses} income={income} budgets={budgets} /></ProtectedPage>} />
        <Route path="/assistant" element={<ProtectedPage authUser={authUser}><AssistantPage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} profile={profile} /></ProtectedPage>} />
        <Route path="/profile" element={<ProtectedPage authUser={authUser}><ProfilePage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} profile={profile} onProfileChange={setProfile} /></ProtectedPage>} />
        <Route path="/settings" element={<ProtectedPage authUser={authUser}><SettingsPage theme={theme} onThemeChange={setTheme} onLogout={handleLogout} settings={settings} onSettingsChange={setSettings} profile={profile} /></ProtectedPage>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}

