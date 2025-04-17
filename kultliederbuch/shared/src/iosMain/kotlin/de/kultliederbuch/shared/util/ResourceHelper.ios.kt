package de.kultliederbuch.shared.util

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS implementation of ResourceHelper
 */
actual object ResourceHelper {
    /**
     * Reads a resource file as string on iOS platform
     */
    actual fun readResourceAsString(path: String): String? {
        val resourcePath = NSBundle.mainBundle.pathForResource(
            path.substringBeforeLast("."),
            path.substringAfterLast(".", ""))
            ?: return null
        
        return NSString.stringWithContentsOfFile(
            resourcePath,
            NSUTF8StringEncoding,
            null
        ) as String?
    }
}
