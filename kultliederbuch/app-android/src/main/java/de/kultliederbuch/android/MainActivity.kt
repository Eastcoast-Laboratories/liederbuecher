package de.kultliederbuch.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction as TextImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties as ComposeDialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Clear
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.kultliederbuch.shared.model.Book
import de.kultliederbuch.shared.model.BookSongPage
import de.kultliederbuch.shared.model.Song
import de.kultliederbuch.shared.repository.CsvImporter
import de.kultliederbuch.shared.repository.CsvSongRepository
import de.kultliederbuch.shared.util.ResourceHelper
import kotlinx.coroutines.runBlocking
import timber.log.Timber

// Datenstruktur für die erweiterten Songs mit Texten und Akkorden
data class SongWithLyrics(
    val title: String,
    val artist: String,
    val lyrics: String,
    val chords: String,
    val book_id: String,
    val book_page: Int?,
    val book_page_notes: Int?,
    val id: String
)

// Helper functions moved outside of any class or function
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

fun getBookColor(buchId: String): Color {
    return getBookColorInfo(buchId).second
}

// Extrahiere einzigartige Akkorde aus einem Akkord-String
fun extractUniqueChords(chordText: String): List<String> {
    // Regulärer Ausdruck, der gängige Akkordmuster erkennt
    // z.B. A, Am, A7, Amaj7, A#, C#m, etc.
    val chordPattern = "[A-G][#b]?(m|maj|min|sus|dim|aug|\\+|-|7|9|11|13|maj7|min7|m7|dim7|aug7|7sus4|add9|madd9)*".toRegex()
    val foundChords = chordPattern.findAll(chordText).map { it.value }.toList()
    
    // Filtere doppelte Akkorde heraus und sortiere sie
    return foundChords.toSet().toList().sorted()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KultliederbuchApp()
        }
    }
}

// SongDetailView moved outside of KultliederbuchApp
@Composable
fun SongDetailView(
    song: Song,
    songWithLyrics: SongWithLyrics?,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClose: () -> Unit,
    songPages: List<BookSongPage>,
    bookMap: Map<String, Book>,
    songComments: Map<String, String>,
    setSongComments: (Map<String, String>) -> Unit,
    currentComment: String,
    setCurrentComment: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val songComment = songComments.getOrDefault(song.id, "")
    val uniqueChords = songWithLyrics?.chords?.let { extractUniqueChords(it) } ?: emptyList()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f) // Not full height, leaves space for search bar
            .background(color = MaterialTheme.colorScheme.background)
            .zIndex(2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorit markieren",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Clear, contentDescription = "Schließen")
                }
            }
            Text(
                text = song.author,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Buchseiten
            if (songPages.isNotEmpty()) {
                // Group pages: without notes first, then with notes
                val pagesWithoutNotes = songPages.filter { !it.bookId.contains("_notes") }
                val pagesWithNotes = songPages.filter { it.bookId.contains("_notes") }
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    (pagesWithoutNotes + pagesWithNotes).forEachIndexed { idx: Int, page: BookSongPage ->
                        val colorInfo = getBookColorInfo(page.bookId)
                        val isNoteBook = page.bookId.contains("_notes")
                        val pageText = if (page.pageNotes != null) page.pageNotes.toString() else page.page?.toString() ?: ""
                        Surface(
                            color = colorInfo.second,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pageText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                if (isNoteBook) {
                                    Text(
                                        text = "🎵",
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Kommentar-Editor (immer sichtbar, editierbar, Zeilenumbrüche erlaubt)
            OutlinedTextField(
                value = currentComment,
                onValueChange = {
                    setCurrentComment(it)
                    val updatedComments = songComments.toMutableMap()
                    updatedComments[song.id] = it
                    setSongComments(updatedComments)
                },
                label = { Text("Kommentar") },
                placeholder = { Text("Dein Kommentar zum Song...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6,
                singleLine = false,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = TextImeAction.Default)
            )
            // Akkorde
            if (uniqueChords.isNotEmpty()) {
                Text("Akkorde:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    uniqueChords.forEach { chord: String ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = chord.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            // Songtext
            if (songWithLyrics != null && songWithLyrics.lyrics.isNotEmpty()) {
                Text("Songtext:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Text(songWithLyrics.lyrics, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Kein Songtext verfügbar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    pages: List<BookSongPage>,
    books: Map<String, Book>,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    songComments: Map<String, String> = emptyMap()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Title, author, comment
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (song.author.isNotEmpty()) {
                    Text(
                        text = song.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                val comment = songComments.getOrDefault(song.id, "")
                if (!comment.isNullOrEmpty()) {
                    Text(
                        text = "Kommentar: ${comment.take(30)}${if (comment.length > 30) "..." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            // Right: Page numbers (vertical), heart below
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 0.dp)
            ) {
                if (pages.isNotEmpty()) {
                    // Group pages: without notes first, then with notes
                    val pagesWithoutNotes = pages.filter { !it.bookId.contains("_notes") }
                    val pagesWithNotes = pages.filter { it.bookId.contains("_notes") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        pagesWithoutNotes.forEach { page ->
                            val colorInfo = getBookColorInfo(page.bookId)
                            val pageText = if (page.pageNotes != null) page.pageNotes.toString() else page.page?.toString() ?: ""
                            Surface(
                                color = colorInfo.second,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pageText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        pagesWithNotes.forEach { page ->
                            val colorInfo = getBookColorInfo(page.bookId)
                            val pageText = if (page.pageNotes != null) page.pageNotes.toString() else page.page?.toString() ?: ""
                            Surface(
                                color = colorInfo.second,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pageText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = "🎵",
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorit markieren",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KultliederbuchApp() {
    val context = LocalContext.current
    val (songs, setSongs) = remember { mutableStateOf(emptyList<Song>()) }
    val (search, setSearch) = remember { mutableStateOf("") }
    val (songPages, setSongPages) = remember { mutableStateOf(emptyMap<String, List<BookSongPage>>()) }
    val (bookMap, setBookMap) = remember { mutableStateOf(emptyMap<String, Book>()) }
    
    val (songsWithLyrics, setSongsWithLyrics) = remember { mutableStateOf(emptyList<SongWithLyrics>()) }
    val (searchInTitle, setSearchInTitle) = remember { mutableStateOf(true) }
    val (searchInAuthor, setSearchInAuthor) = remember { mutableStateOf(true) }
    val (searchInLyrics, setSearchInLyrics) = remember { mutableStateOf(false) }
    val (showOnlyFavorites, setShowOnlyFavorites) = remember { mutableStateOf(false) }
    val (favorites, setFavorites) = remember { mutableStateOf(setOf<String>()) } // Set von Song-IDs
    
    // Song-Detailansicht
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showSongDetails by remember { mutableStateOf(false) }
    
    val (songComments, setSongComments) = remember { mutableStateOf(mapOf<String, String>()) }
    var currentComment by remember { mutableStateOf("") }
    val setCurrentComment = { comment: String -> currentComment = comment }
    
    // Lade Daten beim Start
    LaunchedEffect(key1 = "init") {
        Timber.plant(Timber.DebugTree())
        Timber.d("App gestartet")
        
        // Initialize ResourceHelper with context
        ResourceHelper.init(context)
        
        // Load CSV data and create repository
        val csvData = ResourceHelper.readResourceAsString("data.csv") ?: ""
        val repo = CsvSongRepository(csvData)
        
        runBlocking {
            val allSongs = repo.getAllSongs()
            setSongs(allSongs.filter { it.title.isNotEmpty() })
            
            // Lade Seitenzuordnungen für alle Songs in den Ergebnissen
            val allPages = mutableMapOf<String, List<BookSongPage>>()
            runBlocking {
                allSongs.forEach { song ->
                    allPages[song.id] = repo.getPagesForSong(song.id)
                }
            }
            setSongPages(allPages)
            
            // Lade Bücher
            val books = repo.getAllBooks()
            val booksMap = books.associateBy { it.id }
            setBookMap(booksMap)
            
            // Lade Favoriten aus den SharedPreferences
            val sharedPrefs = context.getSharedPreferences("favorites", 0)
            val favoritesString = sharedPrefs.getString("favorites", "") ?: ""
            if (favoritesString.isNotEmpty()) {
                val favoritesList = favoritesString.split(",")
                setFavorites(favoritesList.toSet())
            }
            
            // Lade Kommentare aus den SharedPreferences
            val commentsString = sharedPrefs.getString("comments", "") ?: ""
            if (commentsString.isNotEmpty()) {
                try {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val commentsMap: Map<String, String> = Gson().fromJson(commentsString, type)
                    setSongComments(commentsMap)
                } catch (e: Exception) {
                    Timber.e(e, "Fehler beim Laden der Kommentare")
                }
            }
            
            // Lade die erweiterten Songs mit Texten aus der JSON-Datei
            try {
                val jsonString = ResourceHelper.readResourceAsString("songs_with_lyrics.json") ?: ""
                if (jsonString.isNotEmpty()) {
                    val type = object : TypeToken<List<SongWithLyrics>>() {}.type
                    val songsList: List<SongWithLyrics> = Gson().fromJson(jsonString, type)
                    setSongsWithLyrics(songsList)
                    Timber.d("${songsList.size} Songs mit Texten geladen")
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Laden der Songs aus JSON")
            }
        }
    }
    
    // Speichere Favoriten, wenn sie sich ändern
    LaunchedEffect(favorites) {
        val sharedPrefs = context.getSharedPreferences("favorites", 0)
        val editor = sharedPrefs.edit()
        editor.putString("favorites", favorites.joinToString(","))
        editor.apply()
    }
    
    // Speichere Kommentare, wenn sie sich ändern
    LaunchedEffect(songComments) {
        val sharedPrefs = context.getSharedPreferences("favorites", 0)
        val editor = sharedPrefs.edit()
        val commentsJson = Gson().toJson(songComments)
        editor.putString("comments", commentsJson)
        editor.apply()
    }
    
    // Filtere Songs basierend auf Suchbegriff
    val filteredSongs = remember(songs, search, searchInTitle, searchInAuthor, searchInLyrics, songsWithLyrics, showOnlyFavorites, favorites) {
        if (search.isEmpty()) {
            if (showOnlyFavorites) {
                songs.filter { favorites.contains(it.id) }
            } else {
                songs
            }
        } else {
            songs.filter { song ->
                // Suche im Titel
                val titleMatch = searchInTitle && song.title.contains(search, ignoreCase = true)
                
                // Suche im Autor
                val authorMatch = searchInAuthor && song.author.contains(search, ignoreCase = true)
                
                // Textsuche in den aufbereiteten Songtexten - nur wenn notwendig
                val lyricsMatch = if (searchInLyrics) {
                    // Suche den aktuellen Song in den JSON-Daten
                    val matchingSongWithLyrics = songsWithLyrics.find { it.title.equals(song.title, ignoreCase = true) && it.artist.equals(song.author, ignoreCase = true) }
                    
                    // Wenn der Song gefunden wurde, prüfe ob der Suchbegriff im Titel ODER im Text enthalten ist
                    if (matchingSongWithLyrics != null) {
                        matchingSongWithLyrics.lyrics.contains(search, ignoreCase = true)
                    } else {
                        false
                    }
                } else {
                    false
                }
                
                // Favoriten-Filter
                val favoriteMatch = !showOnlyFavorites || favorites.contains(song.id)
                
                titleMatch || authorMatch || lyricsMatch && favoriteMatch
            }
        }
    }
    
    // UI-Aufbau
    MaterialTheme(
        colorScheme = lightColorScheme(), // Immer Light Mode verwenden
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Suchleiste
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { setSearch(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Suche nach Titel, Autor oder Text") },
                        singleLine = true,
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { setSearch("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Suche löschen")
                                }
                            }
                        }
                    )
                }
                
                // Checkbox section - individual checkboxes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = searchInTitle,
                            onCheckedChange = { setSearchInTitle(it) },
                            modifier = Modifier.padding(0.dp)
                        )
                        Text(
                            "Titel",
                            modifier = Modifier
                                .clickable { setSearchInTitle(!searchInTitle) }
                                .padding(start = 4.dp)
                        )
                    }
                    
                    // Author checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = searchInAuthor,
                            onCheckedChange = { setSearchInAuthor(it) },
                            modifier = Modifier.padding(0.dp)
                        )
                        Text(
                            "Autor",
                            modifier = Modifier
                                .clickable { setSearchInAuthor(!searchInAuthor) }
                                .padding(start = 4.dp)
                        )
                    }
                    
                    // Lyrics checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = searchInLyrics,
                            onCheckedChange = { setSearchInLyrics(it) },
                            modifier = Modifier.padding(0.dp)
                        )
                        Text(
                            "Text",
                            modifier = Modifier
                                .clickable { setSearchInLyrics(!searchInLyrics) }
                                .padding(start = 4.dp)
                        )
                    }
                    
                    // Favorites checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = showOnlyFavorites,
                            onCheckedChange = { setShowOnlyFavorites(it) },
                            modifier = Modifier.padding(0.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Nur Favoriten anzeigen",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Song-Liste
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    itemsIndexed(filteredSongs) { _, song ->
                        SongItem(
                            song = song,
                            pages = songPages.getOrDefault(song.id, emptyList()),
                            books = bookMap,
                            onClick = {
                                selectedSong = song
                                showSongDetails = true
                                currentComment = songComments.getOrDefault(song.id, "")
                            },
                            isFavorite = favorites.contains(song.id),
                            onFavoriteToggle = {
                                val newFavorites = favorites.toMutableSet()
                                if (favorites.contains(song.id)) {
                                    newFavorites.remove(song.id)
                                } else {
                                    newFavorites.add(song.id)
                                }
                                setFavorites(newFavorites)
                            },
                            songComments = songComments
                        )
                    }
                }
            }
            
            // Song-Detailansicht als Overlay
            if (showSongDetails && selectedSong != null) {
                SongDetailView(
                    song = selectedSong!!,
                    songWithLyrics = songsWithLyrics.find { it.title.equals(selectedSong!!.title, ignoreCase = true) && it.artist.equals(selectedSong!!.author, ignoreCase = true) },
                    isFavorite = favorites.contains(selectedSong!!.id),
                    onFavoriteToggle = {
                        val newFavorites = favorites.toMutableSet()
                        if (favorites.contains(selectedSong!!.id)) {
                            newFavorites.remove(selectedSong!!.id)
                        } else {
                            newFavorites.add(selectedSong!!.id)
                        }
                        setFavorites(newFavorites)
                    },
                    onClose = { showSongDetails = false },
                    songPages = songPages.getOrDefault(selectedSong!!.id, emptyList()),
                    bookMap = bookMap,
                    songComments = songComments,
                    setSongComments = setSongComments,
                    currentComment = currentComment,
                    setCurrentComment = setCurrentComment
                )
            }
        }
    }
}
