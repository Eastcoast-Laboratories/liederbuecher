package de.kultliederbuch.shared.repository

import de.kultliederbuch.shared.db.KultliederbuchDatabase
import de.kultliederbuch.shared.db.Songs
import de.kultliederbuch.shared.db.Books
import de.kultliederbuch.shared.db.Book_song_page
import de.kultliederbuch.shared.model.Song
import de.kultliederbuch.shared.model.Book
import de.kultliederbuch.shared.model.BookSongPage
import de.kultliederbuch.shared.model.UserComment

private fun Songs.toModel() = Song(
    id = id,
    title = title,
    author = author,
    lyrics = lyrics,
    genre = genre,
    year = year?.toInt(),
    favorite = (favorite ?: 0L) != 0L
)

private fun Books.toModel() = Book(
    id = id,
    title = title,
    year = year?.toInt(),
    favorite = (favorite ?: 0L) != 0L
)

private fun Book_song_page.toModel() = BookSongPage(
    songId = song_id,
    bookId = book_id,
    page = page?.toInt(),
    pageNotes = page_notes?.toInt()
)

class DatabaseSongRepository(private val db: KultliederbuchDatabase) : SongRepository {
    private val queries get() = db.kultliederbuchDatabaseQueries

    override suspend fun getAllSongs(): List<Song> = queries.selectAllSongs().executeAsList().map { it.toModel() }
    override suspend fun searchSongs(query: String): List<Song> =
        queries.selectAllSongs().executeAsList().map { it.toModel() }.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.author.contains(query, ignoreCase = true) ||
            it.lyrics.contains(query, ignoreCase = true)
        }
    override suspend fun getSongById(id: String): Song? =
        queries.selectSongById(id).executeAsOneOrNull()?.toModel()
    override suspend fun getAllBooks(): List<Book> = queries.selectAllBooks().executeAsList().map { it.toModel() }
    override suspend fun getBookById(id: String): Book? =
        queries.selectBookById(id).executeAsOneOrNull()?.toModel()
    override suspend fun getPagesForSong(songId: String): List<BookSongPage> =
        queries.selectBySongId(songId).executeAsList().map { it.toModel() }
    override suspend fun getCommentsForSong(songId: String): List<UserComment> = listOf()
    override suspend fun getCommentsForBook(bookId: String): List<UserComment> = listOf()
    override suspend fun setSongFavorite(songId: String, favorite: Boolean) {
        queries.updateSongFavorite(favorite = if (favorite) 1 else 0, id = songId)
    }
}
