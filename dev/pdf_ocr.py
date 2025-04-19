#!/usr/bin/env python3

import os
import subprocess
import re
import json
import logging
import tempfile
import shutil

# Konfiguriere Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger('PDF_OCR_EXTRACTOR')

def extract_text_from_pdf(pdf_path, output_dir):
    """Extract text from PDF using OCR"""
    logger.info(f"Processing PDF with OCR: {pdf_path}")
    
    # Erstelle Basisnamen für die Ausgabedatei
    base_name = os.path.basename(pdf_path)
    book_name = os.path.splitext(base_name)[0]
    output_file = os.path.join(output_dir, f"{book_name}.txt")
    
    # Erstelle temporäres Verzeichnis für die Bildausgabe
    with tempfile.TemporaryDirectory() as temp_dir:
        try:
            # Konvertiere PDF zu Bildern mit pdftoppm (aus poppler-utils)
            cmd_convert = [
                "pdftoppm", 
                "-png",  # PNG-Format
                "-r", "300",  # 300 DPI für bessere OCR-Qualität
                pdf_path,
                os.path.join(temp_dir, "page")
            ]
            subprocess.run(cmd_convert, check=True)
            logger.info(f"Successfully converted {pdf_path} to images")
            
            # Liste der erzeugten Bilddateien
            image_files = sorted([os.path.join(temp_dir, f) for f in os.listdir(temp_dir) if f.endswith('.png')])
            logger.info(f"Generated {len(image_files)} page images")
            
            # OCR mit Tesseract für jede Seite
            all_text = []
            for i, img_file in enumerate(image_files):
                # OCR mit Tesseract
                temp_txt = os.path.join(temp_dir, f"page_{i}.txt")
                cmd_ocr = [
                    "tesseract",
                    img_file,
                    os.path.splitext(temp_txt)[0],  # Tesseract fügt .txt automatisch an
                    "-l", "deu+eng",  # Deutsch und Englisch
                    "--psm", "6"  # Einzelner Block Text (gut für Liedtexte)
                ]
                subprocess.run(cmd_ocr, check=True)
                
                # Seitenu00fcberschrift hinzufu00fcgen
                with open(temp_txt, 'r', encoding='utf-8', errors='replace') as f:
                    page_text = f.read()
                    
                all_text.append(f"[Seite {i+1}]\n{page_text}\n\n")
                
                logger.info(f"Processed page {i+1} of {len(image_files)}")
            
            # Alle Texte in einer Datei zusammenfu00fchren
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(''.join(all_text))
            
            logger.info(f"OCR complete: output saved to {output_file}")
            return output_file
        
        except subprocess.CalledProcessError as e:
            logger.error(f"Error during PDF processing: {e}")
            return None

def parse_song_structure(text_file):
    """Versuche, Songtitel, Künstler und Lyrics aus dem OCR-Text zu extrahieren"""
    logger.info(f"Parsing song structure from {text_file}")
    
    with open(text_file, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()
    
    # Extrahiere Buch-ID aus dem Dateinamen
    base_name = os.path.basename(text_file)
    match = re.search(r'Das Ding (\d+)', base_name)
    book_id = match.group(1) if match else "unknown"
    
    # Teile den Text in Seitenblöcke
    page_pattern = r'\[Seite (\d+)\]([\s\S]+?)(?=\[Seite|$)'
    pages = re.finditer(page_pattern, content)
    
    songs = []
    for page_match in pages:
        page_num = page_match.group(1)
        page_content = page_match.group(2)
        
        # Suche nach Songblöcken in der Seite
        # In Songbüchern ist oft ein Überschriften-Muster erkennbar:
        # Nummer + Titel + Künstler, dann Leerzeile, dann Text
        song_pattern = r'(\d+)[.\s]+(.*?)\s+[-–]\s+(.*?)\n\s*\n([\s\S]+?)(?=\d+[.\s]+|$)'
        song_matches = re.finditer(song_pattern, page_content)
        
        for song_match in song_matches:
            try:
                number = song_match.group(1).strip()
                title = song_match.group(2).strip()
                artist = song_match.group(3).strip()
                lyrics = song_match.group(4).strip()
                
                # Bereinige den Liedtext (entferne überflüssige Leerzeichen und Zeilenumbrüche)
                lyrics = re.sub(r'\s+', ' ', lyrics)
                lyrics = re.sub(r'\n\s*\n', '\n', lyrics)
                
                song = {
                    'number': number,
                    'title': title,
                    'artist': artist,
                    'lyrics': lyrics,
                    'page': int(page_num),
                    'book': f"ding_{book_id}"
                }
                
                songs.append(song)
                logger.info(f"Found song: {title} by {artist} on page {page_num}")
            except Exception as e:
                logger.error(f"Error processing song match: {e}")
    
    # Zweite Methode: Suche nach Mustern basierend auf Inhaltsverzeichnis
    if len(songs) == 0:
        # Suche nach Inhaltsverzeichnis
        toc_pattern = r'(?:Inhaltsverzeichnis|Inhalt)([\s\S]+?)(?:\[Seite|$)'
        toc_match = re.search(toc_pattern, content)
        
        if toc_match:
            toc_content = toc_match.group(1)
            # Typische Inhaltsverzeichniseinträge: Titel ... Seitenzahl
            toc_entries = re.finditer(r'([^\n.]+?)\s*\.+\s*(\d+)', toc_content)
            
            for entry in toc_entries:
                title = entry.group(1).strip()
                page = entry.group(2).strip()
                
                # Suche nach dem Songtext auf der referenzierten Seite
                page_pattern = r'\[Seite ' + page + r'\]([\s\S]+?)(?=\[Seite|$)'
                page_match = re.search(page_pattern, content)
                
                if page_match:
                    page_content = page_match.group(1)
                    
                    # Versuche, den Künstler und den Text zu extrahieren
                    artist_pattern = r'(.+?)\s*[-–]\s*(.+?)\n'
                    artist_match = re.search(artist_pattern, page_content)
                    
                    if artist_match:
                        # Prüfe, welcher Teil wahrscheinlich der Künstler ist
                        part1 = artist_match.group(1).strip()
                        part2 = artist_match.group(2).strip()
                        
                        # Wenn Teil 1 dem Titel ähnlich ist, dann ist Teil 2 wahrscheinlich der Künstler
                        if part1.lower() in title.lower() or title.lower() in part1.lower():
                            artist = part2
                        else:
                            artist = part1
                    else:
                        artist = "Unknown"
                    
                    # Extrahiere den Text (alles nach der ersten Leerzeile)
                    lyrics_pattern = r'\n\s*\n([\s\S]+)'
                    lyrics_match = re.search(lyrics_pattern, page_content)
                    lyrics = lyrics_match.group(1).strip() if lyrics_match else ""
                    
                    song = {
                        'number': "",  # Keine Nummer im Inhaltsverzeichnis
                        'title': title,
                        'artist': artist,
                        'lyrics': lyrics,
                        'page': int(page),
                        'book': f"ding_{book_id}"
                    }
                    
                    songs.append(song)
                    logger.info(f"Found song from TOC: {title} on page {page}")
    
    logger.info(f"Total songs found in {text_file}: {len(songs)}")
    return songs

def process_pdfs(pdf_dir, output_dir):
    """Verarbeite alle 'Das Ding' PDFs im Verzeichnis"""
    os.makedirs(output_dir, exist_ok=True)
    
    # Finde alle Das Ding PDFs
    pdf_files = [f for f in os.listdir(pdf_dir) if f.endswith('.pdf') and 'Das Ding' in f]
    logger.info(f"Found {len(pdf_files)} PDF files to process")
    
    all_songs = []
    
    for pdf_file in pdf_files:
        pdf_path = os.path.join(pdf_dir, pdf_file)
        
        # Konvertiere PDF zu Text mit OCR
        text_file = extract_text_from_pdf(pdf_path, output_dir)
        if not text_file:
            logger.error(f"Failed to extract text from {pdf_file}")
            continue
        
        # Extrahiere Songs aus dem Text
        songs = parse_song_structure(text_file)
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
    
    logger.info("Starting PDF OCR extraction process")
    songs = process_pdfs(pdf_dir, output_dir)
    logger.info(f"OCR extraction complete. Total songs extracted: {len(songs)}")

if __name__ == "__main__":
    main()
