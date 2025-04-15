package de.kultliederbuch.shared.repository

import de.kultliederbuch.shared.model.Song
import de.kultliederbuch.shared.model.Book
import de.kultliederbuch.shared.model.BookSongPage
import de.kultliederbuch.shared.model.UserComment

/**
 * Repository interface for accessing and searching songs, books, and comments.
 */
interface SongRepository {
    suspend fun getAllSongs(): List<Song>
    suspend fun searchSongs(query: String): List<Song>
    suspend fun getSongById(id: String): Song?
    suspend fun getAllBooks(): List<Book>
    suspend fun getBookById(id: String): Book?
    suspend fun getPagesForSong(songId: String): List<BookSongPage>
    suspend fun getCommentsForSong(songId: String): List<UserComment>
    suspend fun getCommentsForBook(bookId: String): List<UserComment>
    // TODO: Add more methods as needed
}

/**
 * Dummy implementation for testing and UI prototyping.
 */
class DummySongRepository : SongRepository {
    private val dummySongs = listOf(
        Song(
            id = "1",
            title = "Über den Wolken",
            author = "Reinhard Mey",
            lyrics = "Über den Wolken muss die Freiheit wohl grenzenlos sein...",
            genre = "Liedermacher",
            year = 1974,
            favorite = true
        ),
        Song(
            id = "2",
            title = "Country Roads",
            author = "John Denver",
            lyrics = "Almost heaven, West Virginia...",
            genre = "Folk",
            year = 1971,
            favorite = false
        )
    )
    override suspend fun getAllSongs() = dummySongs
    override suspend fun searchSongs(query: String): List<Song> =
        dummySongs.filter { it.title.contains(query, ignoreCase = true) ||
            it.author.contains(query, ignoreCase = true) ||
            it.lyrics.contains(query, ignoreCase = true) }
    override suspend fun getSongById(id: String) = dummySongs.find { it.id == id }
    override suspend fun getAllBooks() = listOf<Book>() // TODO: Add dummy books
    override suspend fun getBookById(id: String) = null
    override suspend fun getPagesForSong(songId: String) = listOf<BookSongPage>()
    override suspend fun getCommentsForSong(songId: String) = listOf<UserComment>()
    override suspend fun getCommentsForBook(bookId: String) = listOf<UserComment>()
}
