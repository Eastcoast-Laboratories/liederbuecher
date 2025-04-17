package de.kultliederbuch.android

import android.app.Application
import timber.log.Timber

class KultliederbuchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialisiere Timber nur im Debug-Modus
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.tag("APP_INIT").d("Kultliederbuch application initialized")
        }
    }
}
