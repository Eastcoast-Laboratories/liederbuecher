package de.kultliederbuch.shared.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvImportTest {
    @Test
    fun testImportSongsAndBooksFromCsv() {
        // TODO: Provide a path to a sample CSV or use a test string
        val csvData = """Seite (Noten),Seite,Buch,Künstler,Titel\n1,1,Buch1,Die Ärzte,Schrei nach Liebe\n2,2,Buch2,Die Toten Hosen,Hier kommt Alex""".trimIndent()
        val imported = CsvImporter.import(csvData)
        assertEquals(2, imported.songs.size, "Should import two songs")
        assertEquals(2, imported.books.size, "Should import two books")
        assertEquals("Schrei nach Liebe", imported.songs[0].title)
        assertEquals("Buch1", imported.books[0].title)
    }
}
