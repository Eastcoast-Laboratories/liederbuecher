# Projektname: kultliederbuch

# Allgemeine Beschreibung:
Erstelle ein neues Android-Projekt mit Kotlin und Jetpack Compose, das später mittels Kotlin Multiplatform (KMP) auch für iOS portiert werden soll. Das Projekt "kultliederbuch" speichert die Daten eines Liederbuchs ("Das Ding") lokal und ermöglicht die Suche nach Songtiteln, Autoren und Songtextausschnitten. Für den späteren Einsatz soll ein Shared Module (mit Kernlogik und Datenmodellen) eingerichtet werden. Die lokale Datenhaltung erfolgt z.B. über SQLDelight.

# Ordner- und Modulstruktur:
Erzeuge folgende Ordnerstruktur:
- /kultliederbuch  
  - /app-android  
    - src/main/java/... (Android-spezifischer Code in Kotlin mit Jetpack Compose)  
    - src/main/res/ (Android-Resourcen, inklusive Manifest, Layouts, etc.)  
    - build.gradle.kts (Android-spezifisches Gradle-Skript)  
  - /shared  
    - src/commonMain/kotlin/... (gemeinsamer Code: Datenmodell, Logik, Repository)  
    - src/commonTest/kotlin/... (gemeinsame Tests)  
    - build.gradle.kts (Shared Modul Gradle-Skript)  
  - settings.gradle.kts (Projektspezifische Einstellungen, inkl. Modul-Definitionen)
  - build.gradle.kts (Root-Gradle-Skript)

# Erforderliche Inhalte:
1. **App-Android Modul:**
   - AndroidManifest.xml mit Basis-Konfiguration.
   - MainActivity.kt, die ein grundlegendes Jetpack Compose-Screen mit einer Suchleiste und einer Liste von Songs zeigt (Platzhalter-Code).
   - Abhängigkeiten für Compose einbinden.

2. **Shared Modul:**
   - Ein Kotlin-Datenmodell `Song` (Feldern: id, title, author, text).
   - Eine einfache Search-Funktion, die anhand eines Query-Strings Songs filtert.
   - Platzhalter für Datenbankzugriff mittels SQLDelight (Später erweiterbar).

3. **Projekt-Setup:**
   - Multi-Module-Gradle-Build, sodass die Module korrekt zusammenarbeiten.
   - Minimaler Setup- und Beispielcode, sodass das Projekt in Android Studio lauffähig ist.

# Weitere Hinweise:
- Der Fokus liegt zunächst auf Android (Android Studio). Die KMP-Integration für iOS soll später durch Anpassung des Shared Modules erfolgen.
- Verwende Kotlin DSL für die Gradle-Skripte.
- Binde nur die nötigsten Abhängigkeiten ein, um eine funktionierende Basis zu erzeugen.
- Erstelle sinnvolle Kommentare als Platzhalter für weitere Implementierungen.

Bitte generiere den gesamten Projektordner "kultliederbuch" mit der oben beschriebenen Struktur und den notwendigen Starter-Dateien und Codefragmenten.
