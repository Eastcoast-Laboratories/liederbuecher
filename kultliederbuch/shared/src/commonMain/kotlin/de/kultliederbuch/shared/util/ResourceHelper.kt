package de.kultliederbuch.shared.util

/**
 * Helper class to access resources in a platform-independent way
 */
expect object ResourceHelper {
    /**
     * Reads a resource file as string
     *
     * @param path The path to the resource file
     * @return The content of the resource file as string or null if the file doesn't exist
     */
    fun readResourceAsString(path: String): String?
}
