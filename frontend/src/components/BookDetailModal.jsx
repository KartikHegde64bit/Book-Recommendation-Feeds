import { useState } from 'react'
import * as api from '../services/api'

export default function BookDetailModal({ book, onClose, onImported }) {
  const [loading, setLoading] = useState(false)
  const [importing, setImporting] = useState(false)
  const [importMessage, setImportMessage] = useState('')
  const [details, setDetails] = useState(book)

  const isGoogle = book.source === 'google'

  const handleImport = async () => {
    if (!book.googleId) return
    
    setImporting(true)
    setImportMessage('')
    try {
      const result = await api.importGoogleBook(book.googleId)
      setImportMessage(result.message || 'Book added to your library!')
      if (onImported) {
        onImported(result)
      }
    } catch (err) {
      setImportMessage('Failed to import book')
    } finally {
      setImporting(false)
    }
  }

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose()
    }
  }

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="book-detail-modal">
        <button className="modal-close" onClick={onClose}>&times;</button>
        
        <div className="modal-content">
          <div className="modal-header">
            {details.thumbnail && (
              <img 
                src={details.thumbnail} 
                alt={`${details.title} cover`} 
                className="modal-thumbnail"
              />
            )}
            <div className="modal-title-section">
              <h2>{details.title || 'Untitled'}</h2>
              {details.authors && (
                <p className="modal-authors">by {details.authors}</p>
              )}
              {details.publisher && (
                <p className="modal-publisher">{details.publisher}</p>
              )}
              {details.publishedDate && (
                <p className="modal-date">Published: {details.publishedDate}</p>
              )}
              {details.averageRating && (
                <p className="modal-rating">
                  Rating: {details.averageRating.toFixed(1)} / 5
                  {details.pageCount && ` • ${details.pageCount} pages`}
                </p>
              )}
            </div>
          </div>

          {(details.bookshelves || details.categories) && (
            <div className="modal-categories">
              {(details.bookshelves || details.categories || '').split(';').map((cat, i) => (
                <span key={i} className="category-tag">{cat.trim()}</span>
              ))}
            </div>
          )}

          {details.description && (
            <div className="modal-description">
              <h3>Description</h3>
              <p>{details.description}</p>
            </div>
          )}

          {details.distance !== undefined && (
            <div className="modal-relevance">
              <span className="relevance-label">Match Relevance:</span>
              <span className="relevance-value">
                {Math.max(0, (1 - Math.abs(details.distance)) * 100).toFixed(0)}%
              </span>
            </div>
          )}

          <div className="modal-actions">
            {details.infoLink && (
              <a 
                href={details.infoLink} 
                target="_blank" 
                rel="noopener noreferrer"
                className="btn btn-primary"
              >
                View on Google Books
              </a>
            )}
            {details.buyLink && (
              <a 
                href={details.buyLink} 
                target="_blank" 
                rel="noopener noreferrer"
                className="btn btn-secondary"
              >
                Buy Book
              </a>
            )}
            {isGoogle && (
              <button 
                className="btn btn-accent"
                onClick={handleImport}
                disabled={importing}
              >
                {importing ? 'Adding...' : 'Add to My Library'}
              </button>
            )}
          </div>

          {importMessage && (
            <p className={`import-message ${importMessage.includes('Failed') ? 'error' : 'success'}`}>
              {importMessage}
            </p>
          )}

          <div className="modal-source">
            <span className={`source-indicator ${isGoogle ? 'google' : 'local'}`}>
              {isGoogle ? 'From Google Books' : 'From Your Library'}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}
