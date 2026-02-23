package com.avidreader.controllers;

import com.avidreader.dtos.GoogleBookDTO;
import com.avidreader.services.EmbeddingService;
import com.avidreader.services.GoogleBooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Google Books related operations.
 * Handles fetching book details from Google and importing books to local DB.
 */
@RestController
@RequestMapping("/api/books")
public class GoogleBooksController {

    private final GoogleBooksService googleBooksService;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public GoogleBooksController(GoogleBooksService googleBooksService,
                                  JdbcTemplate jdbcTemplate,
                                  EmbeddingService embeddingService) {
        this.googleBooksService = googleBooksService;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Get detailed information about a book from Google Books API.
     */
    @GetMapping("/google/{volumeId}")
    public ResponseEntity<GoogleBookDTO> getGoogleBookDetails(@PathVariable String volumeId) {
        try {
            GoogleBookDTO book = googleBooksService.getBookDetails(volumeId);
            if (book == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(book);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(null);
        }
    }

    /**
     * Import a book from Google Books into the local database.
     * Generates an embedding for the book and stores it in pgvector.
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importGoogleBook(@RequestBody Map<String, String> request) {
        String googleId = request.get("googleId");
        
        if (googleId == null || googleId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "googleId is required"));
        }

        // Check if book already exists in local DB
        String checkSql = "SELECT id FROM books WHERE google_id = ?";
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, googleId);
        if (!existing.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "id", existing.get(0).get("id"),
                    "message", "Book already exists in database"
            ));
        }

        // Fetch book details from Google
        GoogleBookDTO book = googleBooksService.getBookDetails(googleId);
        if (book == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Book not found on Google Books"));
        }

        // Generate embedding from book metadata
        String textForEmbedding = String.join(" ",
                book.getTitle() != null ? book.getTitle() : "",
                book.getAuthors() != null ? book.getAuthors() : "",
                book.getCategories() != null ? book.getCategories() : "",
                book.getDescription() != null ? book.getDescription().substring(0, Math.min(500, book.getDescription().length())) : ""
        );

        float[] embedding = embeddingService.getEmbedding(textForEmbedding);

        // Convert embedding to string format for pgvector
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        String vectorStr = sb.toString();

        // Insert into database
        String insertSql = """
            INSERT INTO books (type, issued, title, language, authors, subjects, locc, bookshelves, embedding, google_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?)
            RETURNING id
            """;

        Long insertedId = jdbcTemplate.queryForObject(insertSql, Long.class,
                "Text",
                book.getPublishedDate(),
                book.getTitle(),
                book.getLanguage(),
                book.getAuthors(),
                book.getCategories(),
                null,
                book.getCategories(),
                vectorStr,
                googleId
        );

        return ResponseEntity.ok(Map.of(
                "id", insertedId,
                "title", book.getTitle() != null ? book.getTitle() : "",
                "message", "Book imported successfully"
        ));
    }
}
