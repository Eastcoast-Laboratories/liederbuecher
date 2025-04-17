package de.kultliederbuch.android

import android.app.Application
import de.kultliederbuch.shared.util.ResourceHelper
import timber.log.Timber

class KultliederbuchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Timber immer initialisieren für bessere Diagnostik
        Timber.plant(Timber.DebugTree())
        Timber.tag("APP_INIT").d("Kultliederbuch application initialized")
        
        // ResourceHelper mit Kontext initialisieren
        ResourceHelper.init(this)
        Timber.tag("APP_INIT").d("ResourceHelper initialisiert")
    }
}
