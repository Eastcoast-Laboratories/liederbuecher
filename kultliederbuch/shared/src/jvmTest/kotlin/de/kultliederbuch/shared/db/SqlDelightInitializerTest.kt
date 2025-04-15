package de.kultliederbuch.shared.db

import kotlin.test.Test
import kotlin.test.assertEquals
import com.squareup.sqldelight.sqlite.driver.JvmSqliteDriver
import de.kultliederbuch.shared.repository.DatabaseSongRepository
import kotlinx.coroutines.runBlocking

class SqlDelightInitializerTest {
    @Test
    fun testPopulateWithCsvIfEmpty_importsAllSongsAndBooks() {
        val csvData = """Seite (Noten),Seite,Buch,Künstler,Titel\n1,1,Buch1,Die Ärzte,Schrei nach Liebe\n2,2,Buch2,Die Toten Hosen,Hier kommt Alex"""
        val driver = JvmSqliteDriver(KultliederbuchDatabase.Schema, ":memory:")
        val db = KultliederbuchDatabase(driver)
        SqlDelightInitializer.populateWithCsvIfEmpty(db, csvData)
        val repo = DatabaseSongRepository(db)
        val songs = runBlocking { repo.getAllSongs() }
        val books = runBlocking { repo.getAllBooks() }
        assertEquals(2, songs.size, "Should import two songs")
        assertEquals(2, books.size, "Should import two books")
        assertEquals("Schrei nach Liebe", songs[0].title)
        assertEquals("Buch1", books[0].title)
    }
}
