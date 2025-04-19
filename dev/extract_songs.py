#!/usr/bin/env python3

import os
import subprocess
import re
import json
import logging

# Konfiguriere Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger('PDF_SONG_EXTRACTOR')

def convert_pdf_to_text(pdf_path, output_dir):
    """Convert PDF to text using pdftotext"""
    logger.info(f"Processing PDF: {pdf_path}")
    
    # Erstelle Basisnamen für die Ausgabedatei
    base_name = os.path.basename(pdf_path)
    book_name = os.path.splitext(base_name)[0]
    output_file = os.path.join(output_dir, f"{book_name}.txt")
    
    # Konvertiere PDF zu Text
    try:
        # -layout behält das Layout bei
        cmd = ["pdftotext", "-layout", pdf_path, output_file]
        subprocess.run(cmd, check=True)
        logger.info(f"Successfully converted {pdf_path} to {output_file}")
        return output_file
    except subprocess.CalledProcessError as e:
        logger.error(f"Error converting PDF to text: {e}")
        return None

def identify_songs(text_file):
    """Identify songs from the text file"""
    logger.info(f"Identifying songs from {text_file}")
    
    with open(text_file, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()
    
    # Extrahiere Buch-ID aus dem Dateinamen
    base_name = os.path.basename(text_file)
    match = re.search(r'Das Ding (\d+)', base_name)
    book_id = match.group(1) if match else "unknown"
    
    # Pattern-Erkennung für Songs (muss möglicherweise angepasst werden)
    # Typische Songstruktur: Nummer, Titel, Interpret, dann Text
    pattern = r'(\d+)\s+([^\n]+?)\s+(?:- )?([^\n]+?)\s*\n\s*\n([\s\S]+?)(?=\d+\s+[^\n]+|$)'
    
    songs = []
    matches = re.finditer(pattern, content)
    
    for match in matches:
        try:
            number = match.group(1).strip()
            title = match.group(2).strip()
            artist = match.group(3).strip()
            lyrics = match.group(4).strip()
            
            # Manchmal sind Titel und Künstler vertauscht
            if artist.isupper() and not title.isupper():
                title, artist = artist, title
            
            page_number = find_page_number(content, match.start())
            
            song = {
                'number': number,
                'title': title,
                'artist': artist,
                'lyrics': lyrics,
                'page': page_number,
                'book': f"ding_{book_id}"
            }
            
            songs.append(song)
            logger.info(f"Found song: {title} by {artist} on page {page_number}")
        except Exception as e:
            logger.error(f"Error processing song match: {e}")
    
    logger.info(f"Total songs found: {len(songs)}")
    return songs

def find_page_number(content, position):
    """Find the page number for a given position in the text"""
    # Suche nach Seitenzahlen im Format [Seite X]
    lines = content[:position].split('\n')
    for i in range(len(lines) - 1, -1, -1):
        match = re.search(r'\[Seite (\d+)\]', lines[i])
        if match:
            return int(match.group(1))
    
    # Alternative Methode: Zähle die Seitenumbrüche
    page_count = content[:position].count('\f') + 1
    return page_count

def extract_songs_from_pdfs(pdf_dir, output_dir):
    """Extract songs from PDF files"""
    # Stelle sicher, dass das Ausgabeverzeichnis existiert
    os.makedirs(output_dir, exist_ok=True)
    
    # Finde alle PDF-Dateien
    pdf_files = [f for f in os.listdir(pdf_dir) if f.endswith('.pdf') and 'Das Ding' in f]
    
    all_songs = []
    
    for pdf_file in pdf_files:
        pdf_path = os.path.join(pdf_dir, pdf_file)
        
        # Konvertiere PDF zu Text
        text_file = convert_pdf_to_text(pdf_path, output_dir)
        if not text_file:
            continue
        
        # Identifiziere Songs
        songs = identify_songs(text_file)
        all_songs.extend(songs)
    
    # Speichere alle Songs im JSON-Format
    json_output = os.path.join(output_dir, 'all_songs.json')
    with open(json_output, 'w', encoding='utf-8') as f:
        json.dump(all_songs, f, ensure_ascii=False, indent=2)
    
    logger.info(f"Saved {len(all_songs)} songs to {json_output}")
    
    # Erstelle auch eine CSV-Datei für den direkten Import
    csv_output = os.path.join(output_dir, 'songs_export.csv')
    with open(csv_output, 'w', encoding='utf-8') as f:
        # Schreibe CSV-Header
        f.write("Seite (Noten),Seite,Buch,Künstler,Titel\n")
        
        # Schreibe Songs
        for song in all_songs:
            book_id = song['book'].replace('ding_', '')
            f.write(f",{song['page']},{book_id},{song['artist']},{song['title']}\n")
    
    logger.info(f"Saved songs to CSV: {csv_output}")
    return all_songs

def main():
    pdf_dir = "/var/www/kultliederbuch.z11.de/dev"
    output_dir = "/var/www/kultliederbuch.z11.de/dev/extracted"
    
    logger.info("Starting PDF extraction process")
    songs = extract_songs_from_pdfs(pdf_dir, output_dir)
    logger.info(f"Extraction complete. Total songs extracted: {len(songs)}")

if __name__ == "__main__":
    main()
