package de.kultliederbuch.shared.repository

import java.io.File

actual object RepositoryProvider {
    override fun provideSongRepository(): SongRepository {
        // Try to load dev/data.csv from project root
        val csvFile = File("dev/data.csv")
        return if (csvFile.exists()) {
            val csvData = csvFile.readText(Charsets.UTF_8)
            CsvSongRepository(csvData)
        } else {
            DummySongRepository()
        }
    }
}
