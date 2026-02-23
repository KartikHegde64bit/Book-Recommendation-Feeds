import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Preferences from './pages/Preferences'
import Home from './pages/Home'

function App() {
  const { token } = useAuth()

  return (
    <div className="app">
      {token && <Navbar />}
      <main className={`main-content ${token ? 'with-navbar' : ''}`}>
        <Routes>
          <Route path="/login" element={token ? <Navigate to="/" /> : <Login />} />
          <Route path="/signup" element={token ? <Navigate to="/" /> : <Signup />} />
          <Route path="/preferences" element={
            <ProtectedRoute><Preferences /></ProtectedRoute>
          } />
          <Route path="/" element={
            <ProtectedRoute><Home /></ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
