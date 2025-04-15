package de.kultliederbuch.shared.repository

import de.kultliederbuch.shared.model.Song
import de.kultliederbuch.shared.model.Book
import de.kultliederbuch.shared.model.BookSongPage

/**
 * Data class to hold the import result
 */
data class CsvImportResult(
    val songs: List<Song>,
    val books: List<Book>,
    val bookSongPages: List<BookSongPage>
)

/**
 * Simple CSV importer for test and prototyping purposes.
 * Only supports the expected kultliederbuch CSV structure.
 */
object CsvImporter {
    fun import(csv: String): CsvImportResult {
        // Normalize all line endings to \n
        val normalized = csv.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) return CsvImportResult(emptyList(), emptyList(), emptyList())
        val header = lines[0].split(",").map { it.trim() }
        val idxSeiteNoten = header.indexOfFirst { it.contains("Seite (Noten)") }
        val idxSeite = header.indexOfFirst { it == "Seite" }
        val idxBuch = header.indexOfFirst { it == "Buch" }
        val idxKuenstler = header.indexOfFirst { it == "Künstler" }
        val idxTitel = header.indexOfFirst { it == "Titel" }
        if (listOf(idxBuch, idxKuenstler, idxTitel).any { it == -1 }) {
            return CsvImportResult(emptyList(), emptyList(), emptyList())
        }
        val books = mutableMapOf<String, Book>()
        val songs = mutableListOf<Song>()
        val bookSongPages = mutableListOf<BookSongPage>()
        for (line in lines.drop(1)) {
            val cols = line.split(",").map { it.trim() }
            if (cols.size < header.size) continue
            val bookTitle = cols[idxBuch]
            val bookId = bookTitle.replace(" ", "_").lowercase()
            if (!books.containsKey(bookId)) {
                books[bookId] = Book(id = bookId, title = bookTitle, year = null, favorite = false)
            }
            val songTitle = cols[idxTitel]
            val songId = songTitle.replace(" ", "_").lowercase() + "_" + bookId
            val author = cols[idxKuenstler]
            songs.add(
                Song(
                    id = songId,
                    title = songTitle,
                    author = author,
                    lyrics = "",
                    genre = null,
                    year = null,
                    favorite = false
                )
            )
            // Mapping für BookSongPage
            val page = if (idxSeite in cols.indices) cols[idxSeite].toIntOrNull() else null
            val pageNotes = if (idxSeiteNoten in cols.indices) cols[idxSeiteNoten].toIntOrNull() else null
            bookSongPages.add(
                BookSongPage(
                    songId = songId,
                    bookId = bookId,
                    page = page,
                    pageNotes = pageNotes
                )
            )
        }
        return CsvImportResult(songs = songs, books = books.values.toList(), bookSongPages = bookSongPages)
    }
}
