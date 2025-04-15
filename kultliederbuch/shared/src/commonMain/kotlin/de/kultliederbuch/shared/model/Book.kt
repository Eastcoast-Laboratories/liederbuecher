package de.kultliederbuch.shared.model

/**
 * Represents a book entity.
 */
data class Book(
    val id: String,
    val title: String,
    val year: Int?,
    val favorite: Boolean = false
)
