#!/usr/bin/env python3

import os
import re
import json
import csv
from collections import defaultdict
from typing import Dict, List, Tuple, Optional
import logging

# Konfiguration
EXTRACTED_DIR = "/home/runner/work/liederbuecher/liederbuecher/dev/extracted"
OUTPUT_DIR = "/home/runner/work/liederbuecher/liederbuecher/dev/extracted"
CSV_FILE = "/home/runner/work/liederbuecher/liederbuecher/kultliederbuch/app-android/src/main/assets/data.csv"

# Logging einrichten
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Mapping von Buchnamen zu Buchnummern
BOOK_MAPPING = {
    "Das Ding 1 (grün)": "1",
    "Das Ding 2 (rot)": "3",
    "Das Ding 3 (gelb)": "2",
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

# Lade die CSV-Daten und erstelle eine Mapping von Seitenzahlen zu Songs
def load_song_page_mapping() -> Dict[str, Dict[int, List[Tuple[str, str]]]]:
    # Ergebnis-Format: {buchnummer: {seitenzahl: [(song_id, title, artist),...]}}
    songs_by_page = defaultdict(lambda: defaultdict(list))
    song_data = {}  # Format: {song_id: {"title": str, "artist": str, "lyrics": str}}
    
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
                
                # Speichere Song-Daten
                song_data[song_id] = {
                    "title": title,
                    "artist": artist,
                    "lyrics": "",
                    "chords": "",
                    "book_id": book_id,
                    "book_page": None,  # Wird später gesetzt
                    "book_page_notes": None  # Wird später gesetzt
                }
                
                # Füge Seiten hinzu (reguläre Seiten)
                if cols[idx_seite].strip():
                    try:
                        page = int(cols[idx_seite])
                        # Setze Seitenzahl im Song-Objekt
                        song_data[song_id]["book_page"] = page
                        songs_by_page[book_id][page].append((song_id, title, artist))
                    except ValueError:
                        logger.warning(f"Ungültige Seitenzahl: {cols[idx_seite]} für Song {title}")
                
                # Speichere Noten-Seitenzahl, aber verwende sie NICHT für Lyrics-Mapping
                # da der OCR-Text die Liedtexte enthält, nicht die Noten
                if cols[idx_seite_noten].strip():
                    try:
                        page_notes = int(cols[idx_seite_noten])
                        # Setze Noten-Seitenzahl im Song-Objekt (nur für Referenz)
                        song_data[song_id]["book_page_notes"] = page_notes
                    except ValueError:
                        logger.warning(f"Ungültige Notenseitenzahl: {cols[idx_seite_noten]} für Song {title}")
    
    except Exception as e:
        logger.error(f"Fehler beim Laden der CSV-Datei: {e}")
    
    logger.info(f"Geladen: {sum(len(pages) for pages in songs_by_page.values())} Seitenzuordnungen für {len(song_data)} Songs")
    return songs_by_page, song_data

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
    # Lade Song-Seiten-Mapping und Song-Daten
    songs_by_page, song_data = load_song_page_mapping()
    updated_songs = 0
    
    # Verarbeite jede OCR-Textdatei
    for book_name, book_id in BOOK_MAPPING.items():
        ocr_file = os.path.join(EXTRACTED_DIR, f"{book_name}.txt")
        
        if not os.path.exists(ocr_file):
            logger.warning(f"OCR-Datei nicht gefunden: {ocr_file}")
            continue
        
        # Extrahiere Seiteninhalte
        pages = extract_pages_from_ocr(ocr_file)
        
        # Ordne Songtexte zu
        for page_num, page_content in pages.items():
            songs_on_page = songs_by_page.get(book_id, {}).get(page_num, [])
            
            if not songs_on_page:
                # Keine Songs auf dieser Seite gefunden
                continue
            
            logger.info(f"Gefunden: {len(songs_on_page)} Songs auf Seite {page_num} in Buch {book_id}")
            
            # Wenn mehrere Songs auf einer Seite sind, wird der Text allen zugeordnet
            # In einer realen Anwendung würde man hier eine komplexere Logik implementieren
            for song_id, title, artist in songs_on_page:
                if song_id in song_data:
                    cleaned_lyrics, chords = clean_lyrics(page_content)
                    song_data[song_id]["lyrics"] = cleaned_lyrics
                    song_data[song_id]["chords"] = chords
                    # Seitenzahl und Buch nochmal explizit setzen (zur Sicherheit)
                    song_data[song_id]["source_page"] = page_num
                    song_data[song_id]["source_book"] = book_id
                    updated_songs += 1
                    logger.info(f"Songtext zugeordnet: '{title}' von '{artist}' (ID: {song_id})")
    
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
