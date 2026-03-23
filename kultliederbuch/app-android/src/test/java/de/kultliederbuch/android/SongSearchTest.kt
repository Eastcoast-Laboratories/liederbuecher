package de.kultliederbuch.android

import de.kultliederbuch.shared.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchTest {
    @Test
    fun keepsOnGrowingFindsSlippingThroughMyFingers() {
        val songs = listOf(
            Song(
                id = "slipping",
                title = "Slipping through my fingers",
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
                title = "Slipping through my fingers",
                artist = "ABBA",
                lyrics = "Schoolbag in hand she leaves home in the early morning\nSlipping through my fingers all the time\nEach time I think I'm close to knowing\nShe keeps on growing\nSlipping through my fingers all the time",
                chords = "",
                book_id = "5",
                book_page = 305,
                book_page_notes = 305
            )
        )

        val filteredSongs = filterSongsBySearch(
            songs = songs,
            songsWithLyrics = songsWithLyrics,
            search = "keeps on growing",
            searchInTitle = true,
            searchInAuthor = true,
            searchInLyrics = true,
            showOnlyFavorites = false,
            favorites = emptySet()
        )

        assertEquals(1, filteredSongs.size)
        assertTrue(filteredSongs.any { it.title == "Slipping through my fingers" && it.author == "ABBA" })
    }

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
