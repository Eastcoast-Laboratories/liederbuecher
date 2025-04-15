package de.kultliederbuch.shared.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvImportTest {
    @Test
    fun testImportSongsAndBooksFromCsv_variousNewlinesAndSpaces() {
        val csvVariants = listOf(
            // Unix \n
            """Seite (Noten),Seite,Buch,Künstler,Titel
1,1,Buch1,Die Ärzte,Schrei nach Liebe
2,2,Buch2,Die Toten Hosen,Hier kommt Alex""",
            // Windows \r\n
            """Seite (Noten),Seite,Buch,Künstler,Titel
1,1,Buch1,Die Ärzte,Schrei nach Liebe
2,2,Buch2,Die Toten Hosen,Hier kommt Alex""",
            // Gemischt und mit Leerzeichen
            """  Seite (Noten),   Seite , Buch , Künstler , Titel  
 1 , 1 , Buch1 , Die Ärzte , Schrei nach Liebe 
 2 , 2 , Buch2 , Die Toten Hosen , Hier kommt Alex  """
        )
        for (csvData in csvVariants) {
            val imported = CsvImporter.import(csvData)
            assertEquals(2, imported.songs.size, "Should import two songs for variant: $csvData")
            assertEquals(2, imported.books.size, "Should import two books for variant: $csvData")
            assertEquals("Schrei nach Liebe", imported.songs[0].title)
            assertEquals("Buch1", imported.books[0].title)
            // Test BookSongPage-Mappings
            assertEquals(2, imported.bookSongPages.size, "Should import two book-song-page mappings")
            assertEquals("Buch1".replace(" ", "_").lowercase(), imported.bookSongPages[0].bookId)
            assertEquals("Schrei nach Liebe".replace(" ", "_").lowercase() + "_" + "Buch1".replace(" ", "_").lowercase(), imported.bookSongPages[0].songId)
            assertEquals(1, imported.bookSongPages[0].page)
            assertEquals(1, imported.bookSongPages[0].pageNotes)
        }
    }
}
