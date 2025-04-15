package de.kultliederbuch.shared.db

import kotlin.Long
import kotlin.String

public data class Book_song_page(
  public val song_id: String,
  public val book_id: String,
  public val page: Long?,
  public val page_notes: Long?
) {
  public override fun toString(): String = """
  |Book_song_page [
  |  song_id: $song_id
  |  book_id: $book_id
  |  page: $page
  |  page_notes: $page_notes
  |]
  """.trimMargin()
}
