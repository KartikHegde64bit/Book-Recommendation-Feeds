import { useState, useEffect } from 'react'
import * as api from '../services/api'
import BookCard from '../components/BookCard'

export default function Home() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [feed, setFeed] = useState([])
  const [searching, setSearching] = useState(false)
  const [feedLoading, setFeedLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('feed')
  const [feedError, setFeedError] = useState('')

  useEffect(() => {
    api.getFeed()
      .then(data => setFeed(Array.isArray(data) ? data : []))
      .catch(err => {
        setFeed([])
        /* Feed may fail if embedding service is not running — that's OK */
        setFeedError('Recommendation service is unavailable. Make sure the embedding microservice is running.')
      })
      .finally(() => setFeedLoading(false))
  }, [])

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!query.trim()) return
    setSearching(true)
    setActiveTab('search')
    try {
      const data = await api.searchBooks(query)
      setResults(Array.isArray(data) ? data : [])
    } catch {
      setResults([])
    } finally {
      setSearching(false)
    }
  }

  const displayBooks = activeTab === 'search' ? results : feed
  const isLoading = activeTab === 'search' ? searching : feedLoading

  return (
    <div className="home-page">
      <div className="home-container">
        <div className="search-section">
          <h1>Discover Your Next Read</h1>
          <form onSubmit={handleSearch} className="search-form">
            <input
              type="text"
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder="Search for books... (e.g. science fiction space travel)"
              className="search-input"
            />
            <button type="submit" className="btn btn-primary" disabled={searching}>
              {searching ? 'Searching...' : 'Search'}
            </button>
          </form>
        </div>

        <div className="tabs">
          <button
            className={`tab ${activeTab === 'feed' ? 'active' : ''}`}
            onClick={() => setActiveTab('feed')}
          >
            For You
          </button>
          <button
            className={`tab ${activeTab === 'search' ? 'active' : ''}`}
            onClick={() => setActiveTab('search')}
          >
            Search Results
          </button>
        </div>

        <div className="books-section">
          {isLoading ? (
            <div className="page-loading">Loading recommendations...</div>
          ) : displayBooks.length > 0 ? (
            <div className="books-grid">
              {displayBooks.map((book, i) => (
                <BookCard key={book.id || i} book={book} />
              ))}
            </div>
          ) : (
            <div className="empty-state">
              {activeTab === 'feed' ? (
                <>
                  <span className="empty-icon">📚</span>
                  <p>
                    {feedError || 'No recommendations yet. Set your preferences to get personalised suggestions!'}
                  </p>
                </>
              ) : (
                <>
                  <span className="empty-icon">🔍</span>
                  <p>Search for books above to see results here.</p>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
