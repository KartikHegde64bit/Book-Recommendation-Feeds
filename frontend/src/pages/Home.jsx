import { useState, useEffect } from 'react'
import * as api from '../services/api'
import BookCard from '../components/BookCard'
import BookDetailModal from '../components/BookDetailModal'

export default function Home() {
  const [query, setQuery] = useState('')
  const [localResults, setLocalResults] = useState([])
  const [googleResults, setGoogleResults] = useState([])
  const [feed, setFeed] = useState([])
  const [searching, setSearching] = useState(false)
  const [feedLoading, setFeedLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('feed')
  const [feedError, setFeedError] = useState('')
  const [selectedBook, setSelectedBook] = useState(null)

  useEffect(() => {
    api.getFeed()
      .then(data => setFeed(Array.isArray(data) ? data : []))
      .catch(err => {
        setFeed([])
        setFeedError('Recommendation service is unavailable. Make sure the embedding microservice is running.')
      })
      .finally(() => setFeedLoading(false))
  }, [])

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!query.trim()) return
    setSearching(true)
    setActiveTab('local')
    try {
      const data = await api.searchBooks(query)
      // Handle new combined response format
      if (data && typeof data === 'object') {
        setLocalResults(Array.isArray(data.local) ? data.local : [])
        setGoogleResults(Array.isArray(data.google) ? data.google : [])
      } else if (Array.isArray(data)) {
        // Fallback for old format
        setLocalResults(data)
        setGoogleResults([])
      }
    } catch {
      setLocalResults([])
      setGoogleResults([])
    } finally {
      setSearching(false)
    }
  }

  const handleBookClick = async (book) => {
    // If it's a Google book, we might want to fetch more details
    if (book.source === 'google' && book.googleId) {
      try {
        const details = await api.getGoogleBookDetails(book.googleId)
        setSelectedBook({ ...book, ...details })
      } catch {
        setSelectedBook(book)
      }
    } else {
      setSelectedBook(book)
    }
  }

  const handleModalClose = () => {
    setSelectedBook(null)
  }

  const handleBookImported = (result) => {
    // Optionally refresh feed or local results
    console.log('Book imported:', result)
  }

  const getDisplayBooks = () => {
    switch (activeTab) {
      case 'feed':
        return feed
      case 'local':
        return localResults
      case 'google':
        return googleResults
      default:
        return feed
    }
  }

  const isLoading = activeTab === 'feed' ? feedLoading : searching
  const displayBooks = getDisplayBooks()

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
            className={`tab ${activeTab === 'local' ? 'active' : ''}`}
            onClick={() => setActiveTab('local')}
          >
            Library Results {localResults.length > 0 && `(${localResults.length})`}
          </button>
          <button
            className={`tab ${activeTab === 'google' ? 'active' : ''}`}
            onClick={() => setActiveTab('google')}
          >
            Google Books {googleResults.length > 0 && `(${googleResults.length})`}
          </button>
        </div>

        <div className="books-section">
          {isLoading ? (
            <div className="page-loading">Loading recommendations...</div>
          ) : displayBooks.length > 0 ? (
            <div className="books-grid">
              {displayBooks.map((book, i) => (
                <BookCard 
                  key={book.id || book.googleId || i} 
                  book={book} 
                  onClick={handleBookClick}
                />
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
              ) : activeTab === 'local' ? (
                <>
                  <span className="empty-icon">🔍</span>
                  <p>No matching books in your library. Try searching Google Books!</p>
                </>
              ) : (
                <>
                  <span className="empty-icon">🌐</span>
                  <p>Search for books above to see Google Books results.</p>
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {selectedBook && (
        <BookDetailModal 
          book={selectedBook} 
          onClose={handleModalClose}
          onImported={handleBookImported}
        />
      )}
    </div>
  )
}
