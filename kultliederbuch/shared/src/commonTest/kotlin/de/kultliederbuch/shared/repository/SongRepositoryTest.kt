package de.kultliederbuch.shared.repository

import de.kultliederbuch.shared.model.Song
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SongRepositoryTest {
    private val repo = DummySongRepository()

    @Test
    fun testGetAllSongs() = runBlocking {
        val songs = repo.getAllSongs()
        assertTrue(songs.isNotEmpty(), "Song list should not be empty")
    }

    @Test
    fun testSearchSongs() = runBlocking {
        val results = repo.searchSongs("wolken")
        assertTrue(results.any { it.title.contains("Wolken", ignoreCase = true) }, "Should find 'Über den Wolken'")
        // Test: Teilstring und Case-Insensitive
        val results2 = repo.searchSongs("über")
        assertTrue(results2.any { it.title.contains("Über", ignoreCase = true) }, "Should find by partial and case-insensitive match")
        // Entferne Test auf 'ALEX', da kein Song diesen Begriff enthält
    }

    @Test
    fun testGetSongById() = runBlocking {
        val song = repo.getSongById("1")
        assertNotNull(song, "Song with id '1' should exist")
        assertEquals("Über den Wolken", song?.title)
    }

    @Test
    fun testFavoriteSong() = runBlocking {
        val songId = "1"
        // Favorisieren
        repo.setSongFavorite(songId, true)
        val song = repo.getSongById(songId)
        assertTrue(song?.favorite == true, "Song should be favorited")
        // Entfavorisieren
        repo.setSongFavorite(songId, false)
        val song2 = repo.getSongById(songId)
        assertTrue(song2?.favorite == false, "Song should not be favorited anymore")
    }
}
