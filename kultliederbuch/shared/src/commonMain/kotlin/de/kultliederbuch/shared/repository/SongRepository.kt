package de.kultliederbuch.shared.repository

import de.kultliederbuch.shared.model.Song
import de.kultliederbuch.shared.model.Book
import de.kultliederbuch.shared.model.BookSongPage
import de.kultliederbuch.shared.model.UserComment
import de.kultliederbuch.shared.repository.CsvImporter
import kotlinx.coroutines.delay

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
    suspend fun setSongFavorite(songId: String, favorite: Boolean)
    // TODO: Add more methods as needed
}

/**
 * Dummy implementation for testing and UI prototyping.
 */
class DummySongRepository : SongRepository {
    private val dummySongs = mutableListOf(
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
    override suspend fun setSongFavorite(songId: String, favorite: Boolean) {
        val idx = dummySongs.indexOfFirst { it.id == songId }
        if (idx >= 0) {
            val song = dummySongs[idx]
            dummySongs[idx] = song.copy(favorite = favorite)
        }
    }
}

/**
 * Repository implementation that loads songs and books from a CSV string (e.g. from dev/data.csv).
 */
class CsvSongRepository(csvData: String) : SongRepository {
    private val importResult = CsvImporter.import(csvData)
    private val songs = importResult.songs.toMutableList()
    private val books = importResult.books
    private val bookSongPages = importResult.bookSongPages
    
    // Diagnoseinformationen für Debugging
    val diagnosticInfo: String = importResult.diagnosticInfo

    override suspend fun getAllSongs(): List<Song> = songs
    override suspend fun searchSongs(query: String): List<Song> =
        songs.filter { it.title.contains(query, ignoreCase = true) ||
            it.author.contains(query, ignoreCase = true) }
    override suspend fun getSongById(id: String): Song? = songs.find { it.id == id }
    override suspend fun getAllBooks(): List<Book> = books
    override suspend fun getBookById(id: String): Book? = books.find { it.id == id }
    override suspend fun getPagesForSong(songId: String): List<BookSongPage> = 
        bookSongPages.filter { it.songId == songId }
    override suspend fun getCommentsForSong(songId: String): List<UserComment> = listOf()
    override suspend fun getCommentsForBook(bookId: String): List<UserComment> = listOf()
    override suspend fun setSongFavorite(songId: String, favorite: Boolean) {
        val idx = songs.indexOfFirst { it.id == songId }
        if (idx >= 0) {
            val song = songs[idx]
            songs[idx] = song.copy(favorite = favorite)
        }
    }
}
