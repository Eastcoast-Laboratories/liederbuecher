package de.kultliederbuch.shared.model

/**
 * Represents a song entity.
 */
data class Song(
    val id: String,
    val title: String,
    val author: String,
    val lyrics: String,
    val genre: String?,
    val year: Int?,
    val favorite: Boolean = false
)
// TODO: Add support for multiple books per song via join table
