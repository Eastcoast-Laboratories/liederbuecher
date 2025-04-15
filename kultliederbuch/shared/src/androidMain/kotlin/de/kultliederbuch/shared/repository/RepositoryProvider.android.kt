package de.kultliederbuch.shared.repository

actual object RepositoryProvider {
    actual fun provideSongRepository(): SongRepository = DummySongRepository()
}
