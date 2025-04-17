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
    val bookSongPages: List<BookSongPage>,
    val diagnosticInfo: String // Neue Eigenschaft für Diagnose-Informationen
)

/**
 * Simple CSV importer for test and prototyping purposes.
 * Only supports the expected kultliederbuch CSV structure.
 */
object CsvImporter {
    fun import(csv: String): CsvImportResult {
        // Diagnose-Zeichenkette für detaillierte Fehleranalyse
        val diagnosticBuilder = StringBuilder()
        diagnosticBuilder.append("CSV-Länge: ${csv.length} Zeichen\n")
        
        // Kurze Vorschau der CSV-Daten
        val preview = if (csv.length > 100) csv.substring(0, 100) + "..." else csv
        diagnosticBuilder.append("CSV-Vorschau: $preview\n")
        
        // Normalize all line endings to \n
        val normalized = csv.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        diagnosticBuilder.append("Anzahl Zeilen nach Normalisierung: ${lines.size}\n")
        
        if (lines.size <= 1) {
            diagnosticBuilder.append("FEHLER: Keine oder zu wenige Zeilen in der CSV\n")
            return CsvImportResult(emptyList(), emptyList(), emptyList(), diagnosticBuilder.toString())
        }
        
        val header = lines[0].split(",").map { it.trim() }
        diagnosticBuilder.append("Header: ${header.joinToString(", ")}\n")
        
        val idxSeiteNoten = header.indexOfFirst { it.contains("Seite (Noten)") }
        val idxSeite = header.indexOfFirst { it == "Seite" }
        val idxBuch = header.indexOfFirst { it == "Buch" }
        val idxKuenstler = header.indexOfFirst { it == "Künstler" }
        val idxTitel = header.indexOfFirst { it == "Titel" }
        
        diagnosticBuilder.append("Spaltenindizes: Seite (Noten)=$idxSeiteNoten, Seite=$idxSeite, Buch=$idxBuch, Künstler=$idxKuenstler, Titel=$idxTitel\n")
        
        if (listOf(idxBuch, idxKuenstler, idxTitel).any { it == -1 }) {
            diagnosticBuilder.append("FEHLER: Erforderliche Spalten fehlen in der CSV\n")
            return CsvImportResult(emptyList(), emptyList(), emptyList(), diagnosticBuilder.toString())
        }
        
        val books = mutableMapOf<String, Book>()
        val songs = mutableListOf<Song>()
        val bookSongPages = mutableListOf<BookSongPage>()
        
        for (line in lines.drop(1)) {
            val cols = line.split(",").map { it.trim() }
            if (cols.size < header.size) {
                diagnosticBuilder.append("Zeile übersprungen (zu wenige Spalten): $line\n")
                continue
            }
            
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
        
        diagnosticBuilder.append("Ergebnis: ${songs.size} Songs, ${books.size} Bücher, ${bookSongPages.size} Verknüpfungen\n")
        if (songs.isNotEmpty()) {
            diagnosticBuilder.append("Beispiel-Song: ${songs[0].title} von ${songs[0].author}\n")
        }
        
        return CsvImportResult(
            songs = songs, 
            books = books.values.toList(), 
            bookSongPages = bookSongPages,
            diagnosticInfo = diagnosticBuilder.toString()
        )
    }
}
