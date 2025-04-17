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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
