package de.kultliederbuch.shared.util

/**
 * Android implementation of ResourceHelper
 */
actual object ResourceHelper {
    /**
     * Reads a resource file as string on Android platform
     */
    actual fun readResourceAsString(path: String): String? {
        val classLoader = ResourceHelper::class.java.classLoader
        return classLoader?.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
    }
}
