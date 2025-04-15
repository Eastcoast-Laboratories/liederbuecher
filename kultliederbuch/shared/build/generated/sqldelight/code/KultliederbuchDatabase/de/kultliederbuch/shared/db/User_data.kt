package de.kultliederbuch.shared.db

import kotlin.Long
import kotlin.String

public data class User_data(
  public val id: String,
  public val song_id: String?,
  public val book_id: String?,
  public val favorite: Long?,
  public val note: String?
) {
  public override fun toString(): String = """
  |User_data [
  |  id: $id
  |  song_id: $song_id
  |  book_id: $book_id
  |  favorite: $favorite
  |  note: $note
  |]
  """.trimMargin()
}
