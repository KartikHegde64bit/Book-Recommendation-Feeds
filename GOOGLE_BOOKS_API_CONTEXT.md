# Google Books API Integration Context

> **Context document for LLM / Human consumption.**
> Describes the Google Books API integration for the Book Recommendation Engine.

---

## 1. Overview

The Google Books API is integrated to:
1. **Supplement search results** — When users search, results come from both the local pgvector database AND Google Books API.
2. **Provide external book links** — When a user clicks a book card, fetch detailed info from Google and display a link to the book on Google Books.
3. **Enrich local database** — When a user selects a book from Google that doesn't exist locally, add it to the local database with embeddings.

---

## 2. API Configuration

| Key | Value |
|-----|-------|
| **API Key** | `AIzaSyAiwqjwq1glOJ-y9RvClhkmCZ1gaUhnthk` |
| **Base URL** | `https://www.googleapis.com/books/v1` |
| **Primary Endpoint** | `GET /volumes?q={search_terms}&key={API_KEY}` |
| **Volume Detail** | `GET /volumes/{volumeId}?key={API_KEY}` |

---

## 3. Search Query Syntax

The `q` parameter supports free-text search plus special keywords:

| Keyword | Description | Example |
|---------|-------------|---------|
| (none) | Full-text search across all fields | `flowers` |
| `intitle:` | Search in title only | `intitle:flowers` |
| `inauthor:` | Search by author | `inauthor:keyes` |
| `inpublisher:` | Search by publisher | `inpublisher:penguin` |
| `subject:` | Search by category/subject | `subject:fiction` |
| `isbn:` | Search by ISBN | `isbn:9780140449136` |
| `lccn:` | Library of Congress Control Number | `lccn:2001627090` |
| `oclc:` | Online Computer Library Center number | `oclc:123456789` |

**Combined queries:**
```
GET /volumes?q=flowers+inauthor:keyes&key=API_KEY
GET /volumes?q=science+fiction+subject:space&key=API_KEY
```

---

## 4. Response Structure

### 4.1 Volume List Response (`GET /volumes?q=...`)

```json
{
  "kind": "books#volumes",
  "totalItems": 3,
  "items": [
    {
      "kind": "books#volume",
      "id": "_ojXNuzgHRcC",
      "etag": "OTD2tB19qn4",
      "selfLink": "https://www.googleapis.com/books/v1/volumes/_ojXNuzgHRcC",
      "volumeInfo": {
        "title": "Flowers",
        "subtitle": "A subtitle if present",
        "authors": ["Vijaya Khisty Bodach"],
        "publisher": "Publisher Name",
        "publishedDate": "2007",
        "description": "Book description text...",
        "industryIdentifiers": [
          { "type": "ISBN_10", "identifier": "0736867430" },
          { "type": "ISBN_13", "identifier": "9780736867436" }
        ],
        "pageCount": 24,
        "categories": ["Juvenile Nonfiction / Science & Nature / Flowers & Plants"],
        "averageRating": 4.5,
        "ratingsCount": 12,
        "imageLinks": {
          "smallThumbnail": "http://books.google.com/books/content?id=...",
          "thumbnail": "http://books.google.com/books/content?id=..."
        },
        "language": "en",
        "previewLink": "http://books.google.com/books?id=_ojXNuzgHRcC&...",
        "infoLink": "http://books.google.com/books?id=_ojXNuzgHRcC&...",
        "canonicalVolumeLink": "https://books.google.com/books/about/Flowers.html?id=_ojXNuzgHRcC"
      },
      "saleInfo": {
        "country": "US",
        "saleability": "FOR_SALE",
        "isEbook": true,
        "listPrice": { "amount": 9.99, "currencyCode": "USD" },
        "buyLink": "https://play.google.com/store/books/details?id=..."
      },
      "accessInfo": {
        "viewability": "PARTIAL",
        "epub": { "isAvailable": true },
        "pdf": { "isAvailable": false },
        "webReaderLink": "http://play.google.com/books/reader?id=..."
      }
    }
  ]
}
```

### 4.2 Key Fields for Integration

| Field Path | Description | Use Case |
|------------|-------------|----------|
| `items[].id` | Google Volume ID | Unique identifier, use for detail fetch |
| `items[].volumeInfo.title` | Book title | Display |
| `items[].volumeInfo.authors` | Array of author names | Display (join with ", ") |
| `items[].volumeInfo.publisher` | Publisher name | Display |
| `items[].volumeInfo.publishedDate` | Publication date | Display |
| `items[].volumeInfo.description` | Book description | Display in detail view |
| `items[].volumeInfo.categories` | Array of categories | Map to bookshelves |
| `items[].volumeInfo.imageLinks.thumbnail` | Cover image URL | Display |
| `items[].volumeInfo.infoLink` | Google Books page link | "View on Google Books" |
| `items[].volumeInfo.canonicalVolumeLink` | Canonical book URL | Alternative link |
| `items[].volumeInfo.language` | Language code | Filter/display |
| `items[].volumeInfo.pageCount` | Number of pages | Display |
| `items[].volumeInfo.averageRating` | Average rating (1-5) | Display |
| `items[].saleInfo.buyLink` | Purchase link | "Buy" button |

---

## 5. Volume ID & Google Books Site

The `id` field in API responses matches the URL parameter on Google Books site:

```
API Response: "id": "buc0AAAAMAAJ"
↓
Google Books URL: https://books.google.com/ebooks?id=buc0AAAAMAAJ
```

---

## 6. Integration Architecture

### 6.1 Backend (Spring Boot)

```
┌──────────────────────────────────────────────────────────────────┐
│  GoogleBooksService                                               │
│  ├─ searchBooks(query) → List<GoogleBookDTO>                     │
│  │   └─ GET /volumes?q={query}&maxResults=10&key=API_KEY         │
│  │                                                                │
│  └─ getBookDetails(volumeId) → GoogleBookDTO                     │
│      └─ GET /volumes/{volumeId}?key=API_KEY                       │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  RecommendationController (updated)                               │
│  ├─ GET /api/recommend?query=...                                  │
│  │   └─ Returns: { local: [...], google: [...] }                  │
│  │                                                                │
│  └─ GET /api/books/google/{volumeId}                              │
│      └─ Fetch detail from Google, return with links               │
│                                                                   │
│  └─ POST /api/books/import                                        │
│      └─ Import a Google book into local DB with embedding         │
└──────────────────────────────────────────────────────────────────┘
```

### 6.2 Frontend (React)

```
┌──────────────────────────────────────────────────────────────────┐
│  api.js (updated)                                                 │
│  ├─ searchBooks(query) → { local, google }                        │
│  ├─ getGoogleBookDetails(volumeId) → book details                 │
│  └─ importGoogleBook(volumeId) → imported book                    │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  BookCard.jsx (updated)                                           │
│  ├─ onClick → open detail modal / panel                           │
│  └─ Shows "View on Google Books" link for Google results          │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  Home.jsx (updated)                                               │
│  ├─ Display tabs: "Local Results" | "Google Books"               │
│  └─ Handle book selection → fetch details → show links            │
└──────────────────────────────────────────────────────────────────┘
```

---

## 7. Data Mapping

### 7.1 Google Book → Local Book Schema

| Google Field | Local DB Column | Notes |
|-------------|-----------------|-------|
| `id` | `google_id` | New column for deduplication |
| `volumeInfo.title` | `title` | Direct map |
| `volumeInfo.authors` | `authors` | Join with "; " |
| `volumeInfo.categories` | `bookshelves` | Join with "; " |
| `volumeInfo.language` | `language` | Direct map |
| `volumeInfo.publishedDate` | `issued` | Direct map |
| — | `subjects` | Extract from `categories` |
| — | `type` | Default to "Text" |
| — | `locc` | Leave empty |
| — | `embedding` | Generate via EmbeddingService |

### 7.2 Unified Book DTO for Frontend

```json
{
  "id": "123",
  "googleId": "_ojXNuzgHRcC",
  "title": "Flowers",
  "authors": "Vijaya Khisty Bodach",
  "bookshelves": "Juvenile Nonfiction; Science & Nature",
  "language": "en",
  "issued": "2007",
  "distance": 0.15,
  "source": "local|google",
  "thumbnail": "http://...",
  "infoLink": "https://books.google.com/...",
  "description": "..."
}
```

---

## 8. API Endpoints (New/Updated)

### 8.1 Search (Updated)

```
GET /api/recommend?query=science+fiction

Response:
{
  "local": [
    { "id": 1, "title": "...", "authors": "...", "distance": 0.12, "source": "local" }
  ],
  "google": [
    { "googleId": "abc123", "title": "...", "authors": "...", "source": "google", "infoLink": "..." }
  ]
}
```

### 8.2 Google Book Details (New)

```
GET /api/books/google/{volumeId}

Response:
{
  "googleId": "_ojXNuzgHRcC",
  "title": "Flowers",
  "authors": "Vijaya Khisty Bodach",
  "description": "...",
  "thumbnail": "http://...",
  "infoLink": "https://books.google.com/...",
  "buyLink": "https://play.google.com/..."
}
```

### 8.3 Import Google Book (New)

```
POST /api/books/import
Body: { "googleId": "_ojXNuzgHRcC" }

Response:
{ "id": 12345, "title": "Flowers", "message": "Book imported successfully" }
```

---

## 9. Error Handling

| Scenario | HTTP Status | Response |
|----------|-------------|----------|
| Google API error | 502 | `{ "error": "Google Books API unavailable" }` |
| Book not found on Google | 404 | `{ "error": "Book not found" }` |
| Rate limit exceeded | 429 | `{ "error": "Rate limit exceeded, try again later" }` |
| Invalid volume ID | 400 | `{ "error": "Invalid volume ID" }` |

---

## 10. Rate Limits & Best Practices

- **Default quota:** 1,000 requests/day (free tier)
- **Best practices:**
  - Cache Google results on the backend (short TTL, e.g., 5 minutes)
  - Limit `maxResults` to 10-20 per search
  - Use debouncing on frontend search input
  - Batch import operations when possible

---

## 11. Environment Configuration

Add to `application.properties`:

```properties
# Google Books API
google.books.api.key=AIzaSyAiwqjwq1glOJ-y9RvClhkmCZ1gaUhnthk
google.books.api.base-url=https://www.googleapis.com/books/v1
google.books.api.max-results=10
```

---

## 12. Implementation Checklist

### Backend
- [ ] Create `GoogleBooksService` with RestTemplate/WebClient
- [ ] Create `GoogleBookDTO` for API responses
- [ ] Update `RecommendationController` for combined search
- [ ] Add `GET /api/books/google/{volumeId}` endpoint
- [ ] Add `POST /api/books/import` endpoint
- [ ] Add `google_id` column to `books` table
- [ ] Handle Google book import with embedding generation

### Frontend
- [ ] Update `api.js` with Google Books methods
- [ ] Update `BookCard` with click handler and source indicator
- [ ] Create `BookDetailModal` component
- [ ] Update `Home.jsx` for combined results display
- [ ] Add "View on Google Books" link
- [ ] Add "Add to My Library" button for Google books
