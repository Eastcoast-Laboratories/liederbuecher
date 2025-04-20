package de.kultliederbuch.android

import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.kultliederbuch.shared.repository.CsvImporter
import de.kultliederbuch.shared.repository.CsvSongRepository
import de.kultliederbuch.shared.util.ResourceHelper
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity

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
    
    // Datenstruktur für die erweiterten Songs mit Texten und Akkorden
    data class SongWithLyrics(
        val title: String,
        val artist: String,
        val lyrics: String,
        val chords: String,
        val book_id: String,
        val book_page: Int?,
        val book_page_notes: Int?,
        val source_page: Int? = null,
        val source_book: String? = null
    )
    
    val (songsWithLyrics, setSongsWithLyrics) = remember { mutableStateOf(emptyList<SongWithLyrics>()) }
    // Suchfilter-Optionen
    val (searchInTitle, setSearchInTitle) = remember { mutableStateOf(true) }
    val (searchInAuthor, setSearchInAuthor) = remember { mutableStateOf(true) }
    val (searchInLyrics, setSearchInLyrics) = remember { mutableStateOf(false) }
    val (showOnlyFavorites, setShowOnlyFavorites) = remember { mutableStateOf(false) }
    val (favorites, setFavorites) = remember { mutableStateOf(setOf<String>()) } // Set von Song-IDs
    
    // Song-Detailansicht
    var selectedSong by remember { mutableStateOf<de.kultliederbuch.shared.model.Song?>(null) }
    var showSongDetails by remember { mutableStateOf(false) }
    
    val (songComments, setSongComments) = remember { mutableStateOf(mapOf<String, String>()) }
    val (currentComment, setCurrentComment) = remember { mutableStateOf("") }
    val (showCommentInput, setShowCommentInput) = remember { mutableStateOf(false) }

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
            
            // Lade die songs_with_lyrics.json, wenn vorhanden
            try {
                val lyricsJson = ResourceHelper.readResourceAsString("songs_with_lyrics.json")
                if (lyricsJson != null) {
                    val type = object : TypeToken<List<SongWithLyrics>>() {}.type
                    val parsedSongs = Gson().fromJson<List<SongWithLyrics>>(lyricsJson, type)
                    setSongsWithLyrics(parsedSongs)
                    Timber.d("${parsedSongs.size} Songs mit Texten geladen!")
                } else {
                    // Fallback-Daten für Texte
                    Timber.d("Keine songs_with_lyrics.json gefunden. Verwende Dummy-Daten...")
                    setSongsWithLyrics(listOf(
                        SongWithLyrics(
                            title = "American Pie",
                            artist = "Don McLean",
                            lyrics = "Now for ten years, we've been on our own and moss grows fat on a rolling stone...",
                            chords = "G Am C Am Em D G D",
                            book_id = "1",
                            book_page = 150,
                            book_page_notes = 143
                        ),
                        SongWithLyrics(
                            title = "Country Roads",
                            artist = "John Denver",
                            lyrics = "Almost heaven, West Virginia, Blue Ridge Mountains, Shenandoah River...",
                            chords = "G Em D C G",
                            book_id = "1",
                            book_page = 50,
                            book_page_notes = 45
                        )
                    ))
                }
            } catch (e: Exception) {
                Timber.e("Fehler beim Laden der Songtexte: ${e.message}")
                e.printStackTrace()
            }
            
            Timber.d("Loaded ${allSongs.size} songs, ${books.size} books")
        } catch (e: Exception) {
            Timber.e("Error loading data: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Filtere Songs basierend auf Suchkriterien
    val filteredSongs = if (search.length < 3) {
        // Wenn nur Favoriten angezeigt werden sollen, filtere entsprechend
        if (showOnlyFavorites) {
            songs.filter { song -> favorites.contains(song.id) }
        } else {
            songs
        }
    } else {
        // Suche nur ausführen, wenn mindestens 3 Zeichen eingegeben wurden
        val searchFilteredSongs = songs.filter { song ->
            val titleMatch = searchInTitle && song.title.contains(search, ignoreCase = true)
            val authorMatch = searchInAuthor && song.author.contains(search, ignoreCase = true)
            
            // Textsuche in den aufbereiteten Songtexten - nur wenn notwendig
            val lyricsMatch = if (searchInLyrics) {
                // Suche den aktuellen Song in den JSON-Daten
                val matchingSongWithLyrics = songsWithLyrics.find { songWithLyrics ->
                    (songWithLyrics.title.equals(song.title, ignoreCase = true) &&
                     songWithLyrics.artist.equals(song.author, ignoreCase = true))
                }
                
                // Wenn der Song gefunden wurde, prüfe ob der Suchbegriff im Titel ODER im Text enthalten ist
                if (matchingSongWithLyrics != null) {
                    val titleContainsSearch = matchingSongWithLyrics.title.contains(search, ignoreCase = true)
                    val lyricsContainSearch = matchingSongWithLyrics.lyrics.contains(search, ignoreCase = true)
                    
                    if (titleContainsSearch) {
                        Timber.d("'$search' gefunden im Titel: ${matchingSongWithLyrics.title}")
                    }
                    if (lyricsContainSearch) {
                        Timber.d("'$search' gefunden im Text von: ${matchingSongWithLyrics.title}")
                    }
                    
                    titleContainsSearch || lyricsContainSearch
                } else {
                    false
                }
            } else {
                false
            }
            
            titleMatch || authorMatch || lyricsMatch
        }
        
        // Zusaätzlicher Filter für Favoriten
        if (showOnlyFavorites) {
            searchFilteredSongs.filter { song -> favorites.contains(song.id) }
        } else {
            searchFilteredSongs
        }
    }

    @Composable
    fun SongItem(
        song: de.kultliederbuch.shared.model.Song,
        pages: List<de.kultliederbuch.shared.model.BookSongPage>,
        books: Map<String, de.kultliederbuch.shared.model.Book>,
        onClick: () -> Unit,
        isFavorite: Boolean,
        onFavoriteToggle: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = song.author, style = MaterialTheme.typography.bodyMedium)
                }
                
                // Herz-Icon zum Favorisieren
                IconButton(
                    onClick = { onFavoriteToggle() },
                    modifier = Modifier.size(40.dp).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Von Favoriten entfernen" else "Zu Favoriten hinzufügen",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
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

    // Theme auf Light Mode festsetzen
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = MaterialTheme.typography
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hintergrundbild
            Image(
                painter = painterResource(id = R.drawable.app_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
            // Hauptinhalt mit Suchleiste und Detailansicht
            Column(modifier = Modifier.fillMaxSize()) {
                // Suchleiste mit X zum Löschen
                OutlinedTextField(
                    value = search,
                    onValueChange = { setSearch(it) },
                    label = { Text(text = "Suche nach Titel, Autor oder Text ...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "search_bar_a11y" },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { setSearch("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Suche löschen")
                            }
                        }
                    },
                    singleLine = true
                )
                // Filter/Checkboxen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = searchInTitle,
                            onCheckedChange = { isChecked ->
                                if (!isChecked && !searchInAuthor && !searchInLyrics) {
                                    setSearchInTitle(true)
                                    setSearchInAuthor(true)
                                } else {
                                    setSearchInTitle(isChecked)
                                }
                            }
                        )
                        Text("Titel", modifier = Modifier.padding(start = 1.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = searchInAuthor,
                            onCheckedChange = { isChecked ->
                                if (!isChecked && !searchInTitle && !searchInLyrics) {
                                    setSearchInTitle(true)
                                    setSearchInAuthor(true)
                                } else {
                                    setSearchInAuthor(isChecked)
                                }
                            }
                        )
                        Text("Autor", modifier = Modifier.padding(start = 1.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = searchInLyrics,
                            onCheckedChange = { 
                                if (!it && !searchInTitle && !searchInAuthor) {
                                    setSearchInTitle(true)
                                    setSearchInAuthor(true)
                                    setSearchInLyrics(false)
                                } else {
                                    setSearchInLyrics(it)
                                }
                                Timber.d("Textsuche ${if(it) "aktiviert" else "deaktiviert"}")
                            }
                        )
                        Text("Text", modifier = Modifier.padding(start = 1.dp))
                    }
                    // Favoriten-Filter
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = showOnlyFavorites,
                            onCheckedChange = { setShowOnlyFavorites(it) }
                        )
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Nur Favoriten anzeigen",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("", modifier = Modifier.padding(start = 1.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Info zur Suche
                val searchInfoText = if (search.length < 3 && search.isNotEmpty()) {
                    "${songs.size} Lieder (Bitte mind. 3 Zeichen für die Suche eingeben)"
                } else if (search.length >= 3) {
                    "${filteredSongs.size} Lieder gefunden für '${search}'"
                } else {
                    "${songs.size} Lieder"
                }
                Text(text = searchInfoText, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    // Hauptliste
                    if (!showSongDetails) {
                        LazyColumn {
                            itemsIndexed(filteredSongs) { index, song ->
                                SongItem(
                                    song = song,
                                    pages = songPages.getOrDefault(song.id, emptyList()),
                                    books = bookMap,
                                    onClick = {
                                        selectedSong = song
                                        showSongDetails = true
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
                                        Timber.d("Favoriten aktualisiert: ${newFavorites.size} Songs")
                                    }
                                )
                                if (index < filteredSongs.lastIndex) {
                                    Divider()
                                }
                            }
                        }
                    } else {
                        // Detailansicht als Overlay (volle Breite, bis unten)
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
    }

    // Neue SongDetailView Composable
    @Composable
    fun SongDetailView(
        song: Song,
        songWithLyrics: Song?,
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
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                // Kommentar-Editor (immer sichtbar, editierbar, Zeilenumbrüche erlaubt)
                OutlinedTextField(
                    value = currentComment.takeIf { showSongDetails } ?: songComment,
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
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default)
                )
                // Akkorde
                if (uniqueChords.isNotEmpty()) {
                    Text("Akkorde:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        uniqueChords.forEach { chord ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = chord,
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
                    Text("Songtext:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                    Text(songWithLyrics.lyrics, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Kein Songtext verfügbar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
                // Buchseiten
                if (songPages.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        songPages.forEachIndexed { idx, page ->
                            val colorInfo = getBookColorInfo(page.bookId)
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
                                    // Noten-Icon für Buch 2 beim zweiten Eintrag
                                    if (colorInfo.first == "rot" && idx == 1) {
                                        Text(" 🎵", fontSize = 18.sp, modifier = Modifier.padding(start = 2.dp))
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
