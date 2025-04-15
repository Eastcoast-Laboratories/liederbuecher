package de.kultliederbuch.shared.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.Transacter
import de.kultliederbuch.shared.db.KultliederbuchDatabase
import de.kultliederbuch.shared.model.Song
import de.kultliederbuch.shared.model.Book
import de.kultliederbuch.shared.repository.CsvImporter

/**
 * Initializes the database with the songs and books from the CSV on first run.
 * This should be called only once at app start, then the CSV can be deleted.
 * Nach erfolgreichem Import kann die CSV-Datei gelöscht werden.
 */
object SqlDelightInitializer {
    fun populateWithCsvIfEmpty(db: KultliederbuchDatabase, csvData: String) {
        val queries = db.kultliederbuchDatabaseQueries
        if (queries.selectAllSongs().executeAsList().isNotEmpty()) return
        val import = CsvImporter.import(csvData)
        // Insert books
        import.books.forEach { b ->
            queries.insertBook(
                id = b.id,
                title = b.title,
                year = b.year?.toLong(),
                favorite = if (b.favorite) 1 else 0
            )
        }
        // Insert songs
        import.songs.forEach { s ->
            queries.insertSong(
                id = s.id,
                title = s.title,
                author = s.author,
                lyrics = s.lyrics,
                genre = s.genre,
                year = s.year?.toLong(),
                favorite = if (s.favorite) 1 else 0
            )
        }
        // Insert book-song-page mapping if available
        import.bookSongPages.forEach { mapping ->
            queries.insertBookSongPage(
                song_id = mapping.songId,
                book_id = mapping.bookId,
                page = mapping.page?.toLong(),
                page_notes = mapping.pageNotes?.toLong()
            )
        }
    }
}
