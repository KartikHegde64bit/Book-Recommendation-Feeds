export default function BookCard({ book }) {
  return (
    <div className="book-card">
      <div className="book-card-header">
        <h3 className="book-title">{book.title || 'Untitled'}</h3>
      </div>
      <div className="book-card-body">
        {book.authors && (
          <p className="book-authors">
            <span className="label">By:</span> {book.authors}
          </p>
        )}
        {book.bookshelves && (
          <div className="book-shelves">
            {book.bookshelves.split(';').map((shelf, i) => (
              <span key={i} className="shelf-tag">{shelf.trim()}</span>
            ))}
          </div>
        )}
      </div>
      {book.distance !== undefined && (
        <div className="book-card-footer">
          <span className="relevance">
            Relevance: {Math.max(0, (1 - Math.abs(book.distance)) * 100).toFixed(0)}%
          </span>
        </div>
      )}
    </div>
  )
}
