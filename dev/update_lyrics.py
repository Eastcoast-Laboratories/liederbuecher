#!/usr/bin/env python3

import os
import re
import json
import csv
from collections import defaultdict
from typing import Dict, List, Tuple, Optional
import logging

# Konfiguration
EXTRACTED_DIR = "/var/www/kultliederbuch.z11.de/dev/extracted"
OUTPUT_DIR = "/var/www/kultliederbuch.z11.de/dev/extracted"
CSV_FILE = "/var/www/kultliederbuch.z11.de/kultliederbuch/app-android/src/main/assets/data.csv"

# Logging einrichten
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Mapping von Buchnamen zu Buchnummern (CSV book_id)
BOOK_MAPPING = {
    "Das Ding 1 (grün)": "1",
    "Das Ding 2 (rot)": "2",
    "Das Ding 3 (gelb)": "3",
    "Das Ding 4 (blau)": "4",
    "Das Ding 5 (grau)": "5",
    "Weihnachtslieder": "W"
}

# CSV-Parsing mit Unterstützung für Anführungszeichen
def parse_csv_line(line: str) -> List[str]:
    result = []
    in_quotes = False
    current_field = ""
    i = 0
    
    while i < len(line):
        char = line[i]
        if char == '"':
            in_quotes = not in_quotes
            # Doppelte Anführungszeichen im Feld
            if i + 1 < len(line) and line[i + 1] == '"':
                current_field += '"'
                i += 1
        elif char == ',' and not in_quotes:
            result.append(current_field.strip())
            current_field = ""
        else:
            current_field += char
        i += 1
    
    result.append(current_field.strip())
    return result

# Lade die CSV-Daten und erstelle Song-Daten gruppiert nach Buch
def load_song_data() -> Dict[str, dict]:
    # Ergebnis-Format: {song_id: {"title": str, "artist": str, ...}}
    song_data = {}
    
    logger.info(f"Lade Song-Daten aus {CSV_FILE}")
    try:
        with open(CSV_FILE, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            header = parse_csv_line(lines[0])
            
            # Finde Spaltenindizes
            idx_seite_noten = header.index("Seite (Noten)" if "Seite (Noten)" in header else "Seite Noten")
            idx_seite = header.index("Seite")
            idx_buch = header.index("Buch")
            idx_kuenstler = header.index("Künstler")
            idx_titel = header.index("Titel")
            
            for line in lines[1:]:
                cols = parse_csv_line(line)
                if len(cols) < len(header):
                    logger.warning(f"Zeile hat zu wenige Spalten: {line}")
                    continue
                
                title = cols[idx_titel]
                artist = cols[idx_kuenstler]
                book_id = cols[idx_buch]
                
                # Song-ID erstellen
                song_id = f"{title.replace(' ', '_').lower()}_{book_id}"
                
                # Seitenzahlen parsen
                book_page = None
                book_page_notes = None
                if cols[idx_seite].strip():
                    try:
                        book_page = int(cols[idx_seite])
                    except ValueError:
                        pass
                if cols[idx_seite_noten].strip():
                    try:
                        book_page_notes = int(cols[idx_seite_noten])
                    except ValueError:
                        pass
                
                # Speichere Song-Daten
                song_data[song_id] = {
                    "title": title,
                    "artist": artist,
                    "lyrics": "",
                    "chords": "",
                    "book_id": book_id,
                    "book_page": book_page,
                    "book_page_notes": book_page_notes
                }
    
    except Exception as e:
        logger.error(f"Fehler beim Laden der CSV-Datei: {e}")
    
    logger.info(f"Geladen: {len(song_data)} Songs aus CSV")
    return song_data


def normalize_title(title: str) -> str:
    """Normalize a title for fuzzy matching"""
    t = title.upper().strip()
    # Remove common OCR artifacts and punctuation
    t = re.sub(r'[^A-Z0-9ÄÖÜÀÁÂÃÈÉÊÌÍÎÒÓÔÙÚÛÑ\s]', '', t)
    t = re.sub(r'\s+', ' ', t)
    return t.strip()


def get_non_empty_lines(text: str) -> List[str]:
    return [line.strip() for line in text.splitlines() if line.strip()]


def find_song_page_by_title(title: str, artist: str, pages: Dict[int, str], min_page: int = 10) -> Optional[int]:
    """Find the PDF page number where a song title appears at the top of the page.
    Skip intro/TOC pages (<= min_page)."""
    norm_title = normalize_title(title)
    
    for page_num in sorted(pages.keys()):
        if page_num <= min_page:
            continue
        first_lines = ' '.join(get_non_empty_lines(pages[page_num])[:3])
        if not first_lines:
            continue
        if norm_title in normalize_title(first_lines):
            return page_num
    return None

# Extrahiere Seiteninhalte aus einer OCR-Textdatei
def extract_pages_from_ocr(ocr_file: str) -> Dict[int, str]:
    # Format: {page_number: page_content}
    pages = {}
    
    logger.info(f"Verarbeite OCR-Datei: {ocr_file}")
    try:
        with open(ocr_file, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()
            
            # Extrahiere Seiten mit Regex
            page_pattern = r'\[Seite (\d+)\]([\s\S]+?)(?=\[Seite|$)'
            for match in re.finditer(page_pattern, content):
                page_num = int(match.group(1))
                page_content = match.group(2).strip()
                pages[page_num] = page_content
    
    except Exception as e:
        logger.error(f"Fehler beim Verarbeiten der OCR-Datei {ocr_file}: {e}")
    
    logger.info(f"Extrahiert: {len(pages)} Seiten aus {ocr_file}")
    return pages

# Reinige Liedtext-Inhalte
def clean_lyrics(text: str) -> Tuple[str, str]:
    # Entferne Copyright-Hinweise
    text = re.sub(r'\s*[\u00A9\(c\)][^\n]+?(Copyright|Rights Reserved|Secured|Reproduced|permission)[^\n]+', '', text, flags=re.IGNORECASE)
    
    # Entferne M + T: Autorennennungen
    text = re.sub(r'\s*M\s*\+\s*T\s*:[^\n]+', '', text)
    
    # Extrahiere Akkorde
    # Muster: Akkorde wie A, Am, G7, Dsus4, F#m, E7, usw.
    chord_pattern = r'\b([A-G][#b]?(?:maj|min|m|sus|dim|aug|\+|\-|\d)?\d*(?:\/[A-G][#b]?)?)\b'
    
    # Finde Zeilen, die hauptsächlich aus Akkorden bestehen (z.B. "G Am C Am Em D G")
    chord_lines = []
    lines = text.split('\n')
    processed_lines = []
    
    for line in lines:
        # Zähle die Akkorde in der Zeile
        chords = re.findall(chord_pattern, line)
        words = re.findall(r'\b\w+\b', line)
        
        # Wenn die Zeile hauptsächlich aus Akkorden besteht
        if len(chords) > 0 and len(chords) / (len(words) + 0.1) > 0.7:
            chord_lines.append(line.strip())
            # Entferne diese Zeile aus dem Text
            continue
        
        processed_lines.append(line)
    
    # Kombiniere alle gefundenen Akkordzeilen
    chords = ' '.join(chord_lines)
    
    # Bereinige den verbleibenden Text
    cleaned_text = '\n'.join(processed_lines)
    cleaned_text = re.sub(r'\s+', ' ', cleaned_text)
    cleaned_text = re.sub(r'\n\s*\n', '\n', cleaned_text)
    
    return cleaned_text.strip(), chords.strip()

# Hauptfunktion zum Extrahieren und Zuordnen der Songtexte
def update_song_lyrics():
    # Lade Song-Daten aus CSV
    song_data = load_song_data()
    updated_songs = 0
    not_found_songs = []
    
    # Gruppiere Songs nach Buch-ID
    songs_by_book = defaultdict(list)
    for song_id, data in song_data.items():
        songs_by_book[data["book_id"]].append((song_id, data))
    
    # Verarbeite jede OCR-Textdatei
    for book_name, book_id in BOOK_MAPPING.items():
        ocr_file = os.path.join(EXTRACTED_DIR, f"{book_name}.txt")
        
        if not os.path.exists(ocr_file):
            logger.warning(f"OCR-Datei nicht gefunden: {ocr_file}")
            continue
        
        # Extrahiere Seiteninhalte
        pages = extract_pages_from_ocr(ocr_file)
        
        # Songs dieses Buches per Titel in den OCR-Seiten suchen
        book_songs = songs_by_book.get(book_id, [])
        logger.info(f"Buch '{book_name}' (ID={book_id}): {len(book_songs)} Songs zu verarbeiten, {len(pages)} OCR-Seiten")
        
        for song_id, data in book_songs:
            title = data["title"]
            
            # Finde die PDF-Seite anhand des Titels
            pdf_page = find_song_page_by_title(title, data["artist"], pages)
            
            if pdf_page is not None:
                page_content = pages[pdf_page]
                cleaned_lyrics, chords = clean_lyrics(page_content)
                song_data[song_id]["lyrics"] = cleaned_lyrics
                song_data[song_id]["chords"] = chords
                song_data[song_id]["source_page"] = pdf_page
                song_data[song_id]["source_book"] = book_id
                updated_songs += 1
                logger.info(f"MATCH: '{title}' von '{data['artist']}' -> PDF-Seite {pdf_page} (Buch {book_id})")
            else:
                not_found_songs.append((book_id, title, data["artist"]))
                logger.warning(f"NICHT GEFUNDEN: '{title}' von '{data['artist']}' in Buch {book_id}")
    
    logger.info(f"Ergebnis: {updated_songs} zugeordnet, {len(not_found_songs)} nicht gefunden")
    if not_found_songs:
        logger.info(f"Nicht gefundene Songs:")
        for book_id, title, artist in not_found_songs[:20]:
            logger.info(f"  Buch {book_id}: '{title}' von '{artist}'")
        if len(not_found_songs) > 20:
            logger.info(f"  ... und {len(not_found_songs) - 20} weitere")
    
    # Speichere aktualisierte Songs in JSON
    output_file = os.path.join(OUTPUT_DIR, "songs_with_lyrics.json")
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(list(song_data.values()), f, indent=2, ensure_ascii=False)
        logger.info(f"Song-Daten mit Texten gespeichert in: {output_file}")
    except Exception as e:
        logger.error(f"Fehler beim Speichern der Song-Daten: {e}")
    
    # Erstelle auch eine CSV-Datei für den direkten Import
    csv_output = os.path.join(OUTPUT_DIR, 'songs_with_lyrics.csv')
    try:
        with open(csv_output, 'w', encoding='utf-8') as f:
            # CSV-Header
            f.write("Künstler,Titel,Lyrics,Akkorde,Buch,Seite,Seite_Noten\n")
            
            # Schreibe Songs
            for song in song_data.values():
                if song["lyrics"]:
                    artist = song["artist"].replace('"', '""')  # Escape Anführungszeichen
                    title = song["title"].replace('"', '""')
                    lyrics = song["lyrics"].replace('"', '""')
                    chords = song.get("chords", "").replace('"', '""')
                    book_id = song["book_id"]
                    page = song["book_page"] if song["book_page"] is not None else ""
                    page_notes = song["book_page_notes"] if song["book_page_notes"] is not None else ""
                    f.write(f'"{artist}","{title}","{lyrics}","{chords}","{book_id}","{page}","{page_notes}"\n')
        logger.info(f"CSV für Import erstellt: {csv_output}")
    except Exception as e:
        logger.error(f"Fehler beim Erstellen der CSV-Datei: {e}")
    
    return updated_songs

if __name__ == "__main__":
    logger.info("Starte Aktualisierung der Songtexte...")
    updated = update_song_lyrics()
    logger.info(f"Fertig! {updated} Songtexte wurden aktualisiert.")
