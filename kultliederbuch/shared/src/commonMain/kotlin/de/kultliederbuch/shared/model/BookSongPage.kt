package de.kultliederbuch.shared.model

/**
 * Join table: maps a song to a book and its page numbers (with/without notes)
 */
data class BookSongPage(
    val songId: String,
    val bookId: String,
    val page: Int?,           // page number in book
    val pageNotes: Int?       // page number in book with notes
)
