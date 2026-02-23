const API_BASE = '/api'

async function request(endpoint, options = {}) {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: { ...headers, ...options.headers }
  })

  const body = await response.json().catch(() => null)

  if (!response.ok) {
    throw new Error(body?.message || `Request failed (${response.status})`)
  }

  return body
}

/* ---- Auth ---- */

export function signup(data) {
  return request('/auth/signup', { method: 'POST', body: JSON.stringify(data) })
}

export function login(data) {
  return request('/auth/login', { method: 'POST', body: JSON.stringify(data) })
}

/* ---- Preferences ---- */

export function getPreferences() {
  return request('/preferences')
}

export function updatePreferences(tags) {
  return request('/preferences', {
    method: 'PUT',
    body: JSON.stringify({ tags })
  })
}

/* ---- Recommendations ---- */

/**
 * Search books - returns combined results from local DB and Google Books API.
 * @param {string} query - Search query
 * @returns {Promise<{local: Array, google: Array}>}
 */
export function searchBooks(query) {
  return request(`/recommend?query=${encodeURIComponent(query)}`)
}

export function getFeed() {
  return request('/recommend/feed')
}

/* ---- Google Books ---- */

/**
 * Get detailed information about a specific Google Book.
 * @param {string} volumeId - Google Volume ID
 * @returns {Promise<Object>} Book details
 */
export function getGoogleBookDetails(volumeId) {
  return request(`/books/google/${encodeURIComponent(volumeId)}`)
}

/**
 * Import a book from Google Books into the local database.
 * @param {string} googleId - Google Volume ID
 * @returns {Promise<{id: number, title: string, message: string}>}
 */
export function importGoogleBook(googleId) {
  return request('/books/import', {
    method: 'POST',
    body: JSON.stringify({ googleId })
  })
}

