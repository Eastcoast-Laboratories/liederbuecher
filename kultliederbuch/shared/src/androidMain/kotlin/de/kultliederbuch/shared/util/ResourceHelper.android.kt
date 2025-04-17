package de.kultliederbuch.shared.util

import android.content.Context
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Android implementation of ResourceHelper
 */
actual object ResourceHelper {
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
        Timber.tag("RESOURCE_HELPER").d("ResourceHelper initialized with context")
    }
    
    /**
     * Reads a resource file as string on Android platform
     */
    actual fun readResourceAsString(path: String): String? {
        // Versuche verschiedene Methoden, um die Ressource zu laden
        Timber.tag("RESOURCE_HELPER").d("Versuche Ressource zu laden: $path")
        
        // Methode 1: Über den ClassLoader
        val classLoader = ResourceHelper::class.java.classLoader
        var result = classLoader?.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
        if (result != null) {
            Timber.tag("RESOURCE_HELPER").d("Ressource über ClassLoader geladen: $path")
            return result
        }
        
        // Methode 2: Über den App-Context (falls verfügbar)
        appContext?.let { context ->
            try {
                val assetManager = context.assets
                result = assetManager.open(path).bufferedReader().use { it.readText() }
                Timber.tag("RESOURCE_HELPER").d("Ressource über Assets geladen: $path")
                return result
            } catch (e: Exception) {
                Timber.tag("RESOURCE_HELPER").e(e, "Fehler beim Laden der Ressource über Assets: $path")
            }
        }
        
        // Methode 3: Über den Raw-Ordner (falls es sich um eine raw-Ressource handelt)
        appContext?.let { context ->
            try {
                val rawResId = context.resources.getIdentifier(
                    path.substringBeforeLast("."), 
                    "raw",
                    context.packageName
                )
                if (rawResId != 0) {
                    result = context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
                    Timber.tag("RESOURCE_HELPER").d("Ressource über Raw-Ordner geladen: $path")
                    return result
                }
            } catch (e: Exception) {
                Timber.tag("RESOURCE_HELPER").e(e, "Fehler beim Laden der Ressource über Raw: $path")
            }
        }
        
        Timber.tag("RESOURCE_HELPER").e("Konnte Ressource nicht laden: $path")
        return null
    }
}
