package de.kultliederbuch.shared.model

/**
 * Represents a user comment for a song or book.
 */
data class UserComment(
    val id: String,
    val songId: String?, // nullable if comment is only for a book
    val bookId: String?, // nullable if comment is only for a song
    val comment: String,
    val timestamp: Long
)
// TODO: Extend for userId if multi-user support is needed
