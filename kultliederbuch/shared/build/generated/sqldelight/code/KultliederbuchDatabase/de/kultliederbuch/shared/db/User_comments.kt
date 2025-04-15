package de.kultliederbuch.shared.db

import kotlin.Long
import kotlin.String

public data class User_comments(
  public val id: String,
  public val song_id: String?,
  public val book_id: String?,
  public val comment: String,
  public val timestamp: Long
) {
  public override fun toString(): String = """
  |User_comments [
  |  id: $id
  |  song_id: $song_id
  |  book_id: $book_id
  |  comment: $comment
  |  timestamp: $timestamp
  |]
  """.trimMargin()
}
