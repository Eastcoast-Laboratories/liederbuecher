# Konzept für ein KI-driven Projekt: kultliederbuch

# Allgemeine Beschreibung:
Erstelle ein neues Android-Projekt mit Kotlin und Jetpack Compose, das mittels Kotlin Multiplatform (KMP) auch für iOS portiert wird. Das Projekt "kultliederbuch" speichert die Daten einer Liederbuchreihe ("Das Ding") lokal und ermöglicht die Suche nach Songtiteln, Autoren, Bücher und Songtextausschnitten.
- Für den Einsatz soll ein Shared Module (mit Kernlogik und Datenmodellen) eingerichtet werden. 
- Die lokale Datenhaltung erfolgt z.B. über SQLDelight. 
- Es wird der Editor Windsurf von Codeium benutzt mit dem KI-Modell claude 3.7 sonnet in cascade. 
- Die Daten sind schon vorhanden in der tabelle `dev/data.csv`
    Struktur:
    "Seite (Noten)",    "Seite",    "Buch",    "Künstler",    "Titel"
    - Es gibt 12 Bücher: Buch1, Buch2, Buch3, Buch4, Buch5 und BuchW=Weihnachtslieder, jeweils einmal mit Noten, einmal ohne
    - "Seite (Noten)" ist die Seitennummer im Buch<Buch>Noten und Seite die Seitennummer im Buch<Buch>

 - Viele Songtexte sind in PDF Form im Ordner `dev/` vorliegend und könnten per OCR erkannt werden, die weiteren können später aus dem internet gescraped werden.

- Die standardsprache in der app ist Deutsch 
- im code wird ausschliesslich englisch verwendet
- alle ausgegebenen texte werden in strings.xml definiert, wobei nur die default strings.xml erstellt wird, die vollständig in deutsch ist. englisch soll ev. später kommen
 
# Allgemeine Implementierungsanweisungen:
 - **Kotlin Multiplatform (KMP)**
   - Verwende KMP für die gemeinsame Entwicklung von Android und iOS.
   - Teile den Shared Module (mit Kernlogik und Datenmodellen) für beide Plattformen.
   - Verwende SQLDelight für die Datenhaltung.
 - **Test-Driven Development (TDD)**
   - Schreibe für jede neue Funktionalität zuerst einen Test (Unit- oder Integrationstest), dann die Implementierung.
   - Halte die Testabdeckung im Shared-Modul und in der App hoch.

# Schnelle KI-gestützte Umsetzung: Aufgabenübersicht

- [x] Projektstruktur und Gradle-Skripte angelegt
- [x] Datenmodelle (`Song`, `Book`, JoinTable, UserComment) im Shared-Modul
- [x] SQLDelight-Schema mit allen Tabellen (songs, books, JoinTable, user_data, lyrics, user_comments)
- [x] Dummy-Repository und Suchfunktion (mit Beispiel-Songliste)
- [x] MainActivity mit Compose-UI und Dummy-Daten
- [x] Accessibility-Strings in strings.xml (_a11y Platzhalter)
- [x] Beispiel-Test für Suche und User-Kommentare im Shared-Modul (TDD)
- [x] Testabdeckung für alle bisherigen Features (Favoriten, Suche, Kommentare Dummy)
- [ ] Testabdeckung für alle neuen Features (TODO)
- [ ] Integration von Datenimport aus dev/data.csv (TODO)
- [ ] OCR-Import für PDFs vorbereiten (TODO)
- [ ] Fehlerdiagnose für Build und Gradle-Konfiguration automatisieren (TODO)
- [ ] Integrationstests für DatabaseSongRepository (echte DB, nicht nur Dummy)
- [ ] Implementierung und Test von echten User-Kommentaren (persistieren/lesen)
- [ ] Test für Buch-Kommentare analog zu Song-Kommentaren
- [ ] Test für Notizen (user_data.note), falls benötigt
- [ ] Test für Lyrics-Suche, falls Lyrics-Table genutzt wird
- [ ] Import- und Fehlerfall-Tests für CSV und OCR
- [ ] README und strings.xml auf aktuellen Stand halten

# Ordner- und Modulstruktur:
Erzeuge folgende Ordnerstruktur:
- /kultliederbuch  
  - /buildSrc (Für gemeinsame Gradle-Konfigurationen und Versionsverwaltung)
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
   - erweiterte Kotlin-Datenmodelle:
      - `Song` mit folgenden Feldern:
        - id: String (Eindeutige ID)
        - title: String (Titel des Songs)
        - author: String (Autor/Komponist)
        - lyrics: String (Vollständiger Songtext)
        - eine join-tabelle pageNumber -> songId, da ein song in mehreren verschiedenen büchern auftreten kann
        - genre: String? (Genre des Songs)
        - year: Int? (Erscheinungsjahr)
        - favorite: Boolean (Favoriten-Status)
      - `Book` mit folgenden Feldern:
        - id: String (Eindeutige ID)
        - title: String (Titel des Buches)
        - year: Int? (Erscheinungsjahr)
        - favorite: Boolean (Favoriten-Status)
      - `UserComment` (Join-Tabelle für Kommentare des Users zu Songs):
        - id: String (Eindeutige ID)
        - songId: String (Referenz auf Song)
        - bookId: String (Referenz auf Buch)
        - comment: String (Kommentartext)
        - timestamp: Long (Zeitstempel)
   - Eine vollständige Such-Funktionalität mit:
     - Volltextsuche über alle Felder
     - Filteroptionen (Genre, Favoriten, Zeitraum)
     - Sortieroptionen (Alphabetisch, Erscheinungsjahr, Seitennummer)
   - Datenbankzugriff mittels SQLDelight mit folgendem Schema:
     - `songs` (Grunddaten)
     - `books` (Buchdaten)
     - `lyrics` (Volltexte für effiziente Suche)
     - `user_data` (Nutzerspezifische Daten wie Favoriten, Notizen)
     - `user_comments` (Kommentare des Users zu Songs)

3. **Projekt-Setup:**
   - Multi-Module-Gradle-Build, sodass die Module korrekt zusammenarbeiten.
   - Minimaler Setup- und Beispielcode, sodass das Projekt in Android Studio lauffähig ist.

# Funktionalitäten:
1. **Detailansicht**
   - Vollständige Songanzeige mit formatierten Lyrics
   - Akkorde-Anzeige
   - Teilen-Funktion für Songs

2. **Benutzerfunktionen**
   - Favoriten speichern/verwalten
   - Eigene Notizen zu Songs hinzufügen
   - Persönliche Playlists erstellen
   - Kommentare zu Songs und Bücher speichern und anzeigen (UserComment)

# Technische Spezifikationen:
1. **Architektur**
   - Clean Architecture mit klarer Trennung von:
     - Domain Layer (Geschäftslogik und Entitäten)
     - Data Layer (Repositories und Datenquellen)
     - Presentation Layer (UI-Logik mit Compose)

2. **UI/UX**
   - Material Design 3 mit dynamischen Farbschemata
   - Dunkelmodus-Unterstützung
   - Responsive Layouts für verschiedene Bildschirmgrößen

3. **Performance-Optimierungen**
   - Lazy Loading für Songlisten
   - Caching-Strategien für häufig genutzte Daten
   - Paging für große Datenmengen

4. **Testing-Strategie**
   - Test-Driven Development (TDD) als Prinzip: Schreibe zuerst Tests, dann Implementierung
   - Unit-Tests für die Geschäftslogik im Shared Module
   - Integration-Tests für Repository- und Datenbankzugriffe
   - UI-Tests für kritische User Journeys
   - Beispiel-Test für User-Kommentare und Suchfunktion

# AI-Driven Implementierungsansatz:
Das gesamte Projekt wird in einem ganzheitlichen Ansatz implementiert, ohne traditionelle Entwicklungsphasen. Alle Komponenten werden parallel entwickelt:

- Vollständiger Grundcode für Android und Shared Module auf einmal
- Komplettes Datenmodell und Datenbankschema von Anfang an
- Sofortige Integration aller UI-Komponenten und Funktionalitäten
- Direkte Vorbereitung für KMP und iOS-Integration

# Weitere Hinweise:
- Der Fokus liegt zunächst auf Android (Android Studio). Die KMP-Integration für iOS soll später durch Anpassung des Shared Modules erfolgen.
- Verwende Kotlin DSL für die Gradle-Skripte.
- Binde nur die nötigsten Abhängigkeiten ein, um eine funktionierende Basis zu erzeugen.
- Erstelle sinnvolle Kommentare als Platzhalter für weitere Implementierungen.
- Datenschutz und lokale Datenhaltung ohne externe Server
- Zugänglichkeit (Accessibility) durch semantische Beschreibungen

---

**Hinweise für die Umsetzung im Editor:**
- Nutze die Aufgabenübersicht oben für atomic commits und schnelle Navigation.
- Füge überall, wo nötig, `// TODO`-Kommentare für KI und Entwickler ein.
- Lege in `strings.xml` Accessibility-Strings mit `_a11y`-Suffix an.
- Kommentiere wichtige Stellen in Gradle-Skripten und Datenmodellen auf englisch
- Halte die Benennung konsistent und sprechend für optimale KI-Unterstützung.
- Verwende aussagekraftvolle Kommentare in englisch
