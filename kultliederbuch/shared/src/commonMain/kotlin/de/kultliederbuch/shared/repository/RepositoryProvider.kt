package de.kultliederbuch.shared.repository

import de.kultliederbuch.shared.repository.CsvSongRepository
import de.kultliederbuch.shared.repository.DummySongRepository
import de.kultliederbuch.shared.repository.SongRepository

/**
 * Provides the appropriate SongRepository implementation.
 * For production, loads dev/data.csv. For tests or fallback, uses DummySongRepository.
 */
expect object RepositoryProvider {
    fun provideSongRepository(): SongRepository
}
