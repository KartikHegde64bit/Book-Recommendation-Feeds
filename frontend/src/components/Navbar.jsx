import { useAuth } from '../context/AuthContext'
import { useNavigate, useLocation } from 'react-router-dom'
import ThemeToggle from './ThemeToggle'

export default function Navbar() {
  const { username, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <div className="navbar-brand" onClick={() => navigate('/')}>
        <span className="navbar-icon">📚</span>
        <span className="navbar-title">BookRec</span>
      </div>

      <div className="navbar-links">
        <button
          className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
          onClick={() => navigate('/')}
        >
          Home
        </button>
        <button
          className={`nav-link ${location.pathname === '/preferences' ? 'active' : ''}`}
          onClick={() => navigate('/preferences')}
        >
          Preferences
        </button>
      </div>

      <div className="navbar-actions">
        <ThemeToggle />
        <span className="navbar-user">{username}</span>
        <button className="btn btn-ghost" onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  )
}
