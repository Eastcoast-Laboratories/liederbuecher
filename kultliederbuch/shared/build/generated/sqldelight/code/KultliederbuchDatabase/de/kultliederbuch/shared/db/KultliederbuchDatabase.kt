package de.kultliederbuch.shared.db

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver
import de.kultliederbuch.shared.db.shared.newInstance
import de.kultliederbuch.shared.db.shared.schema

public interface KultliederbuchDatabase : Transacter {
  public val kultliederbuchDatabaseQueries: KultliederbuchDatabaseQueries

  public companion object {
    public val Schema: SqlDriver.Schema
      get() = KultliederbuchDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): KultliederbuchDatabase =
        KultliederbuchDatabase::class.newInstance(driver)
  }
}
