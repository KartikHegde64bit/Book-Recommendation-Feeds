-- Migration: Add google_id column to books table
-- Run this script on your PostgreSQL database to add Google Books integration support

-- Add google_id column for tracking books imported from Google Books API
ALTER TABLE books ADD COLUMN IF NOT EXISTS google_id VARCHAR(50);

-- Create unique index on google_id to prevent duplicate imports
CREATE UNIQUE INDEX IF NOT EXISTS idx_books_google_id ON books(google_id) WHERE google_id IS NOT NULL;

-- Optional: Add description column for Google Books descriptions
ALTER TABLE books ADD COLUMN IF NOT EXISTS description TEXT;

-- Optional: Add thumbnail URL column for book covers
ALTER TABLE books ADD COLUMN IF NOT EXISTS thumbnail_url TEXT;
