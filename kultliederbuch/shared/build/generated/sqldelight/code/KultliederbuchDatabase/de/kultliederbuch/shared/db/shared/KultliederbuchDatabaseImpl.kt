package de.kultliederbuch.shared.db.shared

import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver
import de.kultliederbuch.shared.db.KultliederbuchDatabase
import de.kultliederbuch.shared.db.KultliederbuchDatabaseQueries
import kotlin.Int
import kotlin.Unit
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
) : TransacterImpl(driver), KultliederbuchDatabaseQueries
