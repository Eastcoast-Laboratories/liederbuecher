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
    }

    @Test
    fun testGetSongById() = runBlocking {
        val song = repo.getSongById("1")
        assertNotNull(song, "Song with id '1' should exist")
        assertEquals("Über den Wolken", song?.title)
    }
}
