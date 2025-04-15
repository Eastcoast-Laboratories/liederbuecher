package de.kultliederbuch.shared.db.shared

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.`internal`.copyOnWriteList
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import de.kultliederbuch.shared.db.Book_song_page
import de.kultliederbuch.shared.db.Books
import de.kultliederbuch.shared.db.KultliederbuchDatabase
import de.kultliederbuch.shared.db.KultliederbuchDatabaseQueries
import de.kultliederbuch.shared.db.Songs
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.reflect.KClass

internal val KClass<KultliederbuchDatabase>.schema: SqlDriver.Schema
  get() = KultliederbuchDatabaseImpl.Schema

internal fun KClass<KultliederbuchDatabase>.newInstance(driver: SqlDriver): KultliederbuchDatabase =
    KultliederbuchDatabaseImpl(driver)

private class KultliederbuchDatabaseImpl(
  driver: SqlDriver
) : TransacterImpl(driver), KultliederbuchDatabase {
  public override val kultliederbuchDatabaseQueries: KultliederbuchDatabaseQueriesImpl =
      KultliederbuchDatabaseQueriesImpl(this, driver)

  public object Schema : SqlDriver.Schema {
    public override val version: Int
      get() = 1

    public override fun create(driver: SqlDriver): Unit {
      driver.execute(null, """
          |CREATE TABLE songs (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    author TEXT NOT NULL,
          |    lyrics TEXT NOT NULL,
          |    genre TEXT,
          |    year INTEGER,
          |    favorite INTEGER DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE books (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL,
          |    year INTEGER,
          |    favorite INTEGER DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE book_song_page (
          |    song_id TEXT NOT NULL REFERENCES songs(id),
          |    book_id TEXT NOT NULL REFERENCES books(id),
          |    page INTEGER,
          |    page_notes INTEGER,
          |    PRIMARY KEY (song_id, book_id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE lyrics (
          |    song_id TEXT NOT NULL REFERENCES songs(id),
          |    text TEXT NOT NULL,
          |    PRIMARY KEY (song_id)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE user_data (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    song_id TEXT REFERENCES songs(id),
          |    book_id TEXT REFERENCES books(id),
          |    favorite INTEGER DEFAULT 0,
          |    note TEXT
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE user_comments (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    song_id TEXT REFERENCES songs(id),
          |    book_id TEXT REFERENCES books(id),
          |    comment TEXT NOT NULL,
          |    timestamp INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
    }

    public override fun migrate(
      driver: SqlDriver,
      oldVersion: Int,
      newVersion: Int
    ): Unit {
    }
  }
}

private class KultliederbuchDatabaseQueriesImpl(
  private val database: KultliederbuchDatabaseImpl,
  private val driver: SqlDriver
) : TransacterImpl(driver), KultliederbuchDatabaseQueries {
  internal val selectAllSongs: MutableList<Query<*>> = copyOnWriteList()

  internal val selectSongById: MutableList<Query<*>> = copyOnWriteList()

  internal val selectAllBooks: MutableList<Query<*>> = copyOnWriteList()

  internal val selectBookById: MutableList<Query<*>> = copyOnWriteList()

  internal val selectBySongId: MutableList<Query<*>> = copyOnWriteList()

  public override fun <T : Any> selectAllSongs(mapper: (
    id: String,
    title: String,
    author: String,
    lyrics: String,
    genre: String?,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T> = Query(-1407968650, selectAllSongs, driver, "KultliederbuchDatabase.sq",
      "selectAllSongs", "SELECT * FROM songs") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getLong(5),
      cursor.getLong(6)
    )
  }

  public override fun selectAllSongs(): Query<Songs> = selectAllSongs { id, title, author, lyrics,
      genre, year, favorite ->
    Songs(
      id,
      title,
      author,
      lyrics,
      genre,
      year,
      favorite
    )
  }

  public override fun <T : Any> selectSongById(id: String, mapper: (
    id: String,
    title: String,
    author: String,
    lyrics: String,
    genre: String?,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T> = SelectSongByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getLong(5),
      cursor.getLong(6)
    )
  }

  public override fun selectSongById(id: String): Query<Songs> = selectSongById(id) { id_, title,
      author, lyrics, genre, year, favorite ->
    Songs(
      id_,
      title,
      author,
      lyrics,
      genre,
      year,
      favorite
    )
  }

  public override fun <T : Any> selectAllBooks(mapper: (
    id: String,
    title: String,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T> = Query(-1423667422, selectAllBooks, driver, "KultliederbuchDatabase.sq",
      "selectAllBooks", "SELECT * FROM books") { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2),
      cursor.getLong(3)
    )
  }

  public override fun selectAllBooks(): Query<Books> = selectAllBooks { id, title, year, favorite ->
    Books(
      id,
      title,
      year,
      favorite
    )
  }

  public override fun <T : Any> selectBookById(id: String, mapper: (
    id: String,
    title: String,
    year: Long?,
    favorite: Long?
  ) -> T): Query<T> = SelectBookByIdQuery(id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2),
      cursor.getLong(3)
    )
  }

  public override fun selectBookById(id: String): Query<Books> = selectBookById(id) { id_, title,
      year, favorite ->
    Books(
      id_,
      title,
      year,
      favorite
    )
  }

  public override fun <T : Any> selectBySongId(song_id: String, mapper: (
    song_id: String,
    book_id: String,
    page: Long?,
    page_notes: Long?
  ) -> T): Query<T> = SelectBySongIdQuery(song_id) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2),
      cursor.getLong(3)
    )
  }

  public override fun selectBySongId(song_id: String): Query<Book_song_page> =
      selectBySongId(song_id) { song_id_, book_id, page, page_notes ->
    Book_song_page(
      song_id_,
      book_id,
      page,
      page_notes
    )
  }

  public override fun insertSong(
    id: String,
    title: String,
    author: String,
    lyrics: String,
    genre: String?,
    year: Long?,
    favorite: Long?
  ): Unit {
    driver.execute(-742978965,
        """INSERT INTO songs(id, title, author, lyrics, genre, year, favorite) VALUES (?, ?, ?, ?, ?, ?, ?)""",
        7) {
      bindString(1, id)
      bindString(2, title)
      bindString(3, author)
      bindString(4, lyrics)
      bindString(5, genre)
      bindLong(6, year)
      bindLong(7, favorite)
    }
    notifyQueries(-742978965, {database.kultliederbuchDatabaseQueries.selectSongById +
        database.kultliederbuchDatabaseQueries.selectAllSongs})
  }

  public override fun updateSongFavorite(favorite: Long?, id: String): Unit {
    driver.execute(392330039, """UPDATE songs SET favorite = ? WHERE id = ?""", 2) {
      bindLong(1, favorite)
      bindString(2, id)
    }
    notifyQueries(392330039, {database.kultliederbuchDatabaseQueries.selectSongById +
        database.kultliederbuchDatabaseQueries.selectAllSongs})
  }

  public override fun insertBook(
    id: String,
    title: String,
    year: Long?,
    favorite: Long?
  ): Unit {
    driver.execute(-743485377,
        """INSERT INTO books(id, title, year, favorite) VALUES (?, ?, ?, ?)""", 4) {
      bindString(1, id)
      bindString(2, title)
      bindLong(3, year)
      bindLong(4, favorite)
    }
    notifyQueries(-743485377, {database.kultliederbuchDatabaseQueries.selectAllBooks +
        database.kultliederbuchDatabaseQueries.selectBookById})
  }

  public override fun insertBookSongPage(
    song_id: String,
    book_id: String,
    page: Long?,
    page_notes: Long?
  ): Unit {
    driver.execute(-208519517,
        """INSERT INTO book_song_page(song_id, book_id, page, page_notes) VALUES (?, ?, ?, ?)""", 4)
        {
      bindString(1, song_id)
      bindString(2, book_id)
      bindLong(3, page)
      bindLong(4, page_notes)
    }
    notifyQueries(-208519517, {database.kultliederbuchDatabaseQueries.selectBySongId})
  }

  private inner class SelectSongByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T
  ) : Query<T>(selectSongById, mapper) {
    public override fun execute(): SqlCursor = driver.executeQuery(-1660212192,
        """SELECT * FROM songs WHERE id = ?""", 1) {
      bindString(1, id)
    }

    public override fun toString(): String = "KultliederbuchDatabase.sq:selectSongById"
  }

  private inner class SelectBookByIdQuery<out T : Any>(
    public val id: String,
    mapper: (SqlCursor) -> T
  ) : Query<T>(selectBookById, mapper) {
    public override fun execute(): SqlCursor = driver.executeQuery(-1190893580,
        """SELECT * FROM books WHERE id = ?""", 1) {
      bindString(1, id)
    }

    public override fun toString(): String = "KultliederbuchDatabase.sq:selectBookById"
  }

  private inner class SelectBySongIdQuery<out T : Any>(
    public val song_id: String,
    mapper: (SqlCursor) -> T
  ) : Query<T>(selectBySongId, mapper) {
    public override fun execute(): SqlCursor = driver.executeQuery(-1702420000,
        """SELECT * FROM book_song_page WHERE song_id = ?""", 1) {
      bindString(1, song_id)
    }

    public override fun toString(): String = "KultliederbuchDatabase.sq:selectBySongId"
  }
}
