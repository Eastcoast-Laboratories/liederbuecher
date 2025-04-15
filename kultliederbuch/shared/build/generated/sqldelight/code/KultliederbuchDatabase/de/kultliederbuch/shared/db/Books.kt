package de.kultliederbuch.shared.db

import kotlin.Long
import kotlin.String

public data class Books(
  public val id: String,
  public val title: String,
  public val year: Long?,
  public val favorite: Long?
) {
  public override fun toString(): String = """
  |Books [
  |  id: $id
  |  title: $title
  |  year: $year
  |  favorite: $favorite
  |]
  """.trimMargin()
}
