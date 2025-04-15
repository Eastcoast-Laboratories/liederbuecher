package de.kultliederbuch.shared.db

import kotlin.Long
import kotlin.String

public data class Songs(
  public val id: String,
  public val title: String,
  public val author: String,
  public val lyrics: String,
  public val genre: String?,
  public val year: Long?,
  public val favorite: Long?
) {
  public override fun toString(): String = """
  |Songs [
  |  id: $id
  |  title: $title
  |  author: $author
  |  lyrics: $lyrics
  |  genre: $genre
  |  year: $year
  |  favorite: $favorite
  |]
  """.trimMargin()
}
