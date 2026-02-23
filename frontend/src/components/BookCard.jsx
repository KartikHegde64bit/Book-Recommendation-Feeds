export default function BookCard({ book, onClick }) {
  const isGoogle = book.source === 'google'
  
  const handleClick = () => {
    if (onClick) {
      onClick(book)
    }
  }

  return (
    <div className={`book-card ${isGoogle ? 'google-book' : 'local-book'}`} onClick={handleClick}>
      <div className="book-card-header">
        {book.thumbnail && (
          <img 
            src={book.thumbnail} 
            alt={`${book.title} cover`} 
            className="book-thumbnail"
          />
        )}
        <div className="book-title-section">
          <h3 className="book-title">{book.title || 'Untitled'}</h3>
          {isGoogle && <span className="source-badge google">Google Books</span>}
          {!isGoogle && book.source === 'local' && <span className="source-badge local">From Library</span>}
        </div>
      </div>
      <div className="book-card-body">
        {book.authors && (
          <p className="book-authors">
            <span className="label">By:</span> {book.authors}
          </p>
        )}
        {(book.bookshelves || book.categories) && (
          <div className="book-shelves">
            {(book.bookshelves || book.categories || '').split(';').slice(0, 3).map((shelf, i) => (
              <span key={i} className="shelf-tag">{shelf.trim()}</span>
            ))}
          </div>
        )}
        {book.description && (
          <p className="book-description">
            {book.description.length > 150 
              ? book.description.substring(0, 150) + '...' 
              : book.description}
          </p>
        )}
      </div>
      <div className="book-card-footer">
        {book.distance !== undefined && (
          <span className="relevance">
            Relevance: {Math.max(0, (1 - Math.abs(book.distance)) * 100).toFixed(0)}%
          </span>
        )}
        {book.averageRating && (
          <span className="rating">
            Rating: {book.averageRating.toFixed(1)} / 5
          </span>
        )}
        <span className="click-hint">Click for details</span>
      </div>
    </div>
  )
}
