package de.kultliederbuch.shared.db

import kotlin.String

public data class Lyrics(
  public val song_id: String,
  public val text: String
) {
  public override fun toString(): String = """
  |Lyrics [
  |  song_id: $song_id
  |  text: $text
  |]
  """.trimMargin()
}
