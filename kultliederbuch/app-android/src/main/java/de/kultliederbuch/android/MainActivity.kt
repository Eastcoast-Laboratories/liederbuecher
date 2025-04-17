package de.kultliederbuch.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import de.kultliederbuch.shared.repository.CsvSongRepository
import de.kultliederbuch.shared.util.ResourceHelper
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // CSV-Daten laden und ersten 10 Titel loggen
        val csvData = ResourceHelper.readResourceAsString("data.csv")
        Timber.tag("CSV_DATA").d("CSV-Daten geladen (null=nicht gefunden): ${csvData?.take(100)}...")
        
        // Zusätzliche Diagnoseinformationen über den Ladeprozess
        if (csvData == null) {
            // Versuche, alle Dateien im Assets-Verzeichnis aufzulisten
            try {
                val assetList = assets.list("")
                Timber.tag("CSV_FILES").d("Verfügbare Assets: ${assetList?.joinToString(", ")}")
            } catch (e: Exception) {
                Timber.tag("CSV_FILES").e("Fehler beim Auflisten der Assets: ${e.message}")
            }
            
            Timber.tag("CSV_ERROR").e("CSV-Datei konnte nicht geladen werden. Geprüfte Pfade: assets/data.csv, raw/data, klassische Ressource")
        }
        
        val repo = CsvSongRepository(csvData ?: "")
        
        // Diagnoseinformationen aus dem Importer loggen
        Timber.tag("CSV_DIAGNOSIS").d("CSV-Import Diagnoseinformationen:\n${repo.diagnosticInfo}")
        
        // Hier die ersten 10 Titel loggen
        runBlocking {
            try {
                val allSongs = repo.getAllSongs()
                val firstTen = allSongs.take(10)
                Timber.tag("CSV_IMPORT").d("Importierte ${allSongs.size} Songs insgesamt")
                firstTen.forEachIndexed { index, song ->
                    Timber.tag("CSV_SONG").d("Song $index: ${song.title} - ${song.author}")
                }
            } catch (e: Exception) {
                Timber.tag("CSV_ERROR").e(e, "Fehler beim Laden der Songs")
            }
        }
        
        setContent {
            KultliederbuchApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KultliederbuchApp() {
    // Lade CSV-Daten aus den Ressourcen
    val csvData = remember {
        ResourceHelper.readResourceAsString("data.csv") ?: ""
    }

    // remember verwenden, um State über Recompositions hinweg zu erhalten
    var search by remember { mutableStateOf("") }
    val repo = remember { CsvSongRepository(csvData) }
    var songs by remember { mutableStateOf(listOf<de.kultliederbuch.shared.model.Song>()) }

    LaunchedEffect(search) {
        songs = runBlocking { repo.searchSongs(search) }
        // Log bei Suchen
        if (search.isNotEmpty()) {
            Timber.tag("SEARCH").d("Suche nach '$search' ergab ${songs.size} Ergebnisse")
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(text = "Suche nach Titel, Autor oder Text...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "search_bar_a11y" }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Lieder", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(songs.size) { idx ->
                    val song = songs[idx]
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = song.author, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
