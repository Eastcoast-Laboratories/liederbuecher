package de.kultliederbuch.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.kultliederbuch.shared.repository.CsvImporter
import de.kultliederbuch.shared.repository.CsvSongRepository
import de.kultliederbuch.shared.util.ResourceHelper
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File

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
    val context = LocalContext.current
    val (songs, setSongs) = remember { mutableStateOf(emptyList<de.kultliederbuch.shared.model.Song>()) }
    val (search, setSearch) = remember { mutableStateOf("") }
    val (songPages, setSongPages) = remember { mutableStateOf(emptyMap<String, List<de.kultliederbuch.shared.model.BookSongPage>>()) }
    val (bookMap, setBookMap) = remember { mutableStateOf(emptyMap<String, de.kultliederbuch.shared.model.Book>()) }
    val (ocrContentMap, setOcrContentMap) = remember { mutableStateOf(emptyMap<String, String>()) }
    val (searchInTitle, setSearchInTitle) = remember { mutableStateOf(true) }
    val (searchInAuthor, setSearchInAuthor) = remember { mutableStateOf(true) }
    val (searchInLyrics, setSearchInLyrics) = remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        Timber.d("Loading data...")
        try {
            // Lade CSV-Daten aus den Ressourcen
            val csvData = ResourceHelper.readResourceAsString("data.csv") ?: ""
            Timber.d("CSV-Daten geladen: ${csvData.take(50)}...")
            val repo = CsvSongRepository(csvData)
            
            // Lade Songs und Buchinformationen
            val allSongs = runBlocking { repo.getAllSongs() }
            setSongs(allSongs.filter { it.title.isNotEmpty() })
            
            // Lade Seitenzuordnungen für alle Songs in den Ergebnissen
            val allPages = mutableMapOf<String, List<de.kultliederbuch.shared.model.BookSongPage>>()
            runBlocking {
                allSongs.forEach { song ->
                    allPages[song.id] = repo.getPagesForSong(song.id)
                }
            }
            setSongPages(allPages)
            
            // Lade Buchinformationen
            val books = runBlocking { repo.getAllBooks() }
            setBookMap(books.associateBy { it.id })
            
            // Lade OCR-Inhalte für Textsuche
            if (searchInLyrics) {
                // Für Demo nur Dummy-OCR-Daten
                val ocrTextMap = mutableMapOf<String, String>(
                    "Das Ding 1 (grün)" to "Liedtext Country Roads: Almost heaven, West Virginia, Blue Ridge Mountains, Shenandoah River",
                    "Das Ding 2 (rot)" to "Liedtext für ein anderes Lied mit Text heaven auch hier",
                    "Das Ding 3 (gelb)" to "One moment in time when I'm more than I thought I could be"
                )
                setOcrContentMap(ocrTextMap)
                Timber.d("Dummy OCR-Daten für die Demo geladen")
                Timber.d("OCR-Daten: ${ocrTextMap.keys.joinToString()}")
                ocrTextMap.forEach { (book, text) ->
                    Timber.d("OCR-Inhalt für $book (${text.length} Zeichen): ${text.take(50)}...")
                }
            }
            
            Timber.d("Loaded ${allSongs.size} songs, ${books.size} books")
        } catch (e: Exception) {
            Timber.e("Error loading data: ${e.message}")
            e.printStackTrace()
        }
    }
    
    val filteredSongs = songs.filter { song ->
        if (search.isEmpty()) {
            true
        } else {
            val titleMatch = searchInTitle && song.title.contains(search, ignoreCase = true)
            val authorMatch = searchInAuthor && song.author.contains(search, ignoreCase = true)
            val lyricsMatch = if (searchInLyrics) {
                // Hole alle OCR-Texte und prüfe, ob einer den Suchbegriff enthält
                val searchResult = ocrContentMap.any { (bookName, content) ->
                    val found = content.contains(search, ignoreCase = true)
                    if (found) {
                        Timber.d("Text '$search' gefunden in Buch: $bookName")
                    }
                    found
                }
                searchResult
            } else false
            
            Timber.d("Suche nach '$search': Titel=$titleMatch, Autor=$authorMatch, Text=$lyricsMatch")
            
            titleMatch || authorMatch || lyricsMatch
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { setSearch(it) },
                label = { Text(text = "Suche nach Titel, Autor oder Text...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "search_bar_a11y" }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = searchInTitle,
                        onCheckedChange = { setSearchInTitle(it) }
                    )
                    Text("Titel", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = searchInAuthor,
                        onCheckedChange = { setSearchInAuthor(it) }
                    )
                    Text("Autor", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = searchInLyrics,
                        onCheckedChange = { 
                            setSearchInLyrics(it)
                            Timber.d("Textsuche ${if(it) "aktiviert" else "deaktiviert"}")
                        }
                    )
                    Text("Text", modifier = Modifier.padding(start = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "${filteredSongs.size} Lieder", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(filteredSongs.size) { idx ->
                    val song = filteredSongs[idx]
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                                Text(text = song.author, style = MaterialTheme.typography.bodyMedium)
                            }
                            val pages = songPages[song.id] ?: emptyList()
                            val colorToPages = mutableMapOf<String, Pair<Int?, Int?>>()
                            pages.forEach { page ->
                                val (colorName, _) = getBookColorInfo(page.bookId)
                                val prev = colorToPages[colorName] ?: (null to null)
                                if (page.page != null) {
                                    colorToPages[colorName] = page.page to prev.second
                                } else if (page.pageNotes != null) {
                                    colorToPages[colorName] = prev.first to page.pageNotes
                                }
                            }
                            colorToPages.forEach { (colorName, seiten) ->
                                val color = getBookColorInfo(pages.find { getBookColorInfo(it.bookId).first == colorName }?.bookId ?: "").second
                                val seite = seiten.first
                                val seiteNoten = seiten.second
                                val seitenText = if (colorName == "W") {
                                    (seite ?: seiteNoten)?.toString() ?: ""
                                } else {
                                    buildString {
                                        if (seite != null) append(seite)
                                        if (seiteNoten != null) {
                                            if (isNotEmpty()) append("/")
                                            append(seiteNoten)
                                        }
                                    }
                                }
                                if (seitenText.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .background(color, shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = seitenText,
                                            color = if (colorName == "gelb") Color.Black else Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getBookColorInfo(buchId: String): Pair<String, Color> {
    val nummer = Regex("book_(\\w+)").find(buchId)?.groupValues?.get(1)?.replace("_notes", "") ?: ""
    return when (nummer) {
        "1" -> "grün" to Color(0xFF4CAF50)
        "2" -> "rot" to Color(0xFFF44336)
        "3" -> "gelb" to Color(0xFFFFEB3B)
        "4" -> "blau" to Color(0xFF2196F3)
        "5" -> "grau" to Color(0xFF9E9E9E)
        "W" -> "W" to Color(0xFF9C27B0)
        else -> "?" to Color.LightGray
    }
}
