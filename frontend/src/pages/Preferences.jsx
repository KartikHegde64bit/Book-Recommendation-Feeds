import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import * as api from '../services/api'

const AVAILABLE_TAGS = [
  'Fiction', 'Science Fiction', 'Fantasy', 'Mystery', 'Romance',
  'Adventure', 'Horror', 'Thriller', 'Historical Fiction', 'Drama',
  'Poetry', 'Philosophy', 'Science', 'Mathematics', 'History',
  'Politics', 'Biography', 'Autobiography', 'Travel', 'Exploration',
  'Children\'s Literature', 'Young Adult', 'Religion', 'Art', 'Music',
  'Psychology', 'Sociology', 'Economics', 'Technology', 'Nature',
  'Humor', 'Essays', 'Letters', 'Speeches', 'Law',
  'Medicine', 'Cooking', 'Architecture', 'Photography', 'Education'
]

export default function Preferences() {
  const [selected, setSelected] = useState(new Set())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    api.getPreferences()
      .then(tags => {
        if (Array.isArray(tags)) setSelected(new Set(tags))
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const toggleTag = (tag) => {
    setSelected(prev => {
      const next = new Set(prev)
      if (next.has(tag)) next.delete(tag)
      else next.add(tag)
      return next
    })
  }

  const handleSave = async () => {
    setSaving(true)
    setMessage('')
    try {
      await api.updatePreferences([...selected])
      setMessage('Preferences saved!')
      setTimeout(() => navigate('/'), 1500)
    } catch (err) {
      setMessage('Failed to save preferences')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="page-loading">Loading preferences...</div>

  return (
    <div className="preferences-page">
      <div className="preferences-container">
        <div className="preferences-header">
          <h1>Your Reading Preferences</h1>
          <p>Select genres and topics you enjoy. We'll use these to recommend books for you.</p>
        </div>
        <div className="tags-grid">
          {AVAILABLE_TAGS.map(tag => (
            <button
              key={tag}
              className={`tag-chip ${selected.has(tag) ? 'selected' : ''}`}
              onClick={() => toggleTag(tag)}
            >
              {tag}
            </button>
          ))}
        </div>
        <div className="preferences-actions">
          <span className="selected-count">{selected.size} selected</span>
          {message && (
            <span className={`save-message ${message.includes('Failed') ? 'error' : ''}`}>
              {message}
            </span>
          )}
          <button
            className="btn btn-primary"
            onClick={handleSave}
            disabled={saving || selected.size === 0}
          >
            {saving ? 'Saving...' : 'Save Preferences'}
          </button>
        </div>
      </div>
    </div>
  )
}
