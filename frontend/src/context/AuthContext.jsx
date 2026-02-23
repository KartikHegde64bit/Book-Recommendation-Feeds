import { createContext, useContext, useState, useCallback } from 'react'
import * as api from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [username, setUsername] = useState(localStorage.getItem('username'))

  const login = useCallback(async (credentials) => {
    const data = await api.login(credentials)
    localStorage.setItem('token', data.access_token)
    localStorage.setItem('username', credentials.username)
    setToken(data.access_token)
    setUsername(credentials.username)
    return data
  }, [])

  const signup = useCallback(async (userData) => {
    return await api.signup(userData)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    setToken(null)
    setUsername(null)
  }, [])

  return (
    <AuthContext.Provider value={{ token, username, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
