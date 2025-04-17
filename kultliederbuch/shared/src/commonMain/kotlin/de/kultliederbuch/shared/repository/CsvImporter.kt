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
            val buchId = cols[idxBuch]
            
            // Erstelle zwei Bucheinträge: Mit und ohne Noten
            val bookWithoutNotesId = "book_${buchId}"
            val bookWithNotesId = "book_${buchId}_notes"
            
            // Nur hinzufügen, wenn das Buch noch nicht existiert
            if (!books.containsKey(bookWithoutNotesId)) {
                books[bookWithoutNotesId] = Book(
                    id = bookWithoutNotesId,
                    title = if (buchId == "W") "Weihnachtslieder" else "Buch $buchId", 
                    year = null, 
                    favorite = false
                )
            }
            
            if (!books.containsKey(bookWithNotesId)) {
                books[bookWithNotesId] = Book(
                    id = bookWithNotesId,
                    title = if (buchId == "W") "Weihnachtslieder mit Noten" else "Buch $buchId mit Noten", 
                    year = null, 
                    favorite = false
                )
            }
            
            val songTitle = cols[idxTitel]
            val songId = songTitle.replace(" ", "_").lowercase() + "_" + buchId
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
            // Mapping für BookSongPage - für Buch ohne Noten
            val page = if (idxSeite in cols.indices) cols[idxSeite].toIntOrNull() else null
            if (page != null) {
                bookSongPages.add(
                    BookSongPage(
                        songId = songId,
                        bookId = bookWithoutNotesId,
                        page = page,
                        pageNotes = null
                    )
                )
            }
            
            // Mapping für BookSongPage - für Buch mit Noten
            val pageNotes = if (idxSeiteNoten in cols.indices) cols[idxSeiteNoten].toIntOrNull() else null
            if (pageNotes != null) {
                bookSongPages.add(
                    BookSongPage(
                        songId = songId,
                        bookId = bookWithNotesId,
                        page = null,
                        pageNotes = pageNotes
                    )
                )
            }
        }
        return CsvImportResult(songs = songs, books = books.values.toList(), bookSongPages = bookSongPages)
    }
}
