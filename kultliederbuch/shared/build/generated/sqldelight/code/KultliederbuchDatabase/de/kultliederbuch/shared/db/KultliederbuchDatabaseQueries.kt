package de.kultliederbuch.shared.db

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.Unit

public interface KultliederbuchDatabaseQueries : Transacter {
  public fun <T : Any> selectAllSongs(mapper: (
    id: String,
    title: String,
    author: String,
    lyrics: String,
    genre: String?,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T>

  public fun selectAllSongs(): Query<Songs>

  public fun <T : Any> selectSongById(id: String, mapper: (
    id: String,
    title: String,
    author: String,
    lyrics: String,
    genre: String?,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T>

  public fun selectSongById(id: String): Query<Songs>

  public fun <T : Any> selectAllBooks(mapper: (
    id: String,
    title: String,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T>

  public fun selectAllBooks(): Query<Books>

  public fun <T : Any> selectBookById(id: String, mapper: (
    id: String,
    title: String,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T>

  public fun selectBookById(id: String): Query<Books>

  public fun <T : Any> selectBySongId(song_id: String, mapper: (
    song_id: String,
    book_id: String,
    page: Long?,
    page_notes: Long?
  ) -> T): Query<T>

  public fun selectBySongId(song_id: String): Query<Book_song_page>

  public fun insertSong(
    id: String,
    title: String,
    author: String,
    lyrics: String,
    genre: String?,
    year: Long?,
    favorite: Long?
  ): Unit

  public fun updateSongFavorite(favorite: Long?, id: String): Unit

  public fun insertBook(
    id: String,
    title: String,
    year: Long?,
    favorite: Long?
  ): Unit

  public fun insertBookSongPage(
    song_id: String,
    book_id: String,
    page: Long?,
    page_notes: Long?
  ): Unit
}
