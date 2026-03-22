package de.kultliederbuch.android

import de.kultliederbuch.shared.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchTest {
    @Test
    fun angry2FindsMammaMiaInLyricsSearch() {
        val songs = listOf(
            Song(
                id = "mamma-mia",
                title = "Mamma Mia",
                author = "ABBA",
                lyrics = "",
                genre = null,
                year = null,
                favorite = false
            ),
            Song(
                id = "other-song",
                title = "Other Song",
                author = "Other Artist",
                lyrics = "",
                genre = null,
                year = null,
                favorite = false
            )
        )

        val songsWithLyrics = listOf(
            SongWithLyrics(
                title = "Mamma Mia",
                artist = "ABBA",
                lyrics = "I’ve been angry and sad about the things that you do.",
                chords = "",
                book_id = "2",
                book_page = 297,
                book_page_notes = 309
            )
        )

        val filteredSongs = filterSongsBySearch(
            songs = songs,
            songsWithLyrics = songsWithLyrics,
            search = "angry2",
            searchInTitle = false,
            searchInAuthor = false,
            searchInLyrics = true,
            showOnlyFavorites = false,
            favorites = emptySet()
        )

        assertEquals(1, filteredSongs.size)
        assertTrue(filteredSongs.any { it.title == "Mamma Mia" && it.author == "ABBA" })
    }
}
