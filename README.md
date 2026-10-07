# DywanMag — Ekspozycja Dywanów (React SPA)

System zarządzania ekspozycją dywanów na stojakach sklepowych (magazyny 1a..20b), zintegrowany z bazą danych Google Firebase Firestore, skanerem kodów kreskowych EAN/ESL oraz wyszukiwarką specyfikacji produktów leroymerlin.pl.

## Funkcjonalności

- **Zarządzanie stojakami ekspozycyjnymi**: Wizualizacja 20 stojaków (40 miejsc ekspozycyjnych: sloty A i B).
- **Synchronizacja Real-time**: Dwukierunkowa synchronizacja z bazą Firebase Firestore w czasie rzeczywistym z natychmiastowym buforem lokalnym.
- **Skaner kodów ESL / EAN**: Obsługa kamery internetowej / mobilnej (HTML5 QR/Barcode Scanner) oraz szybkie wprowadzanie kodów z czytników USB/Bluetooth.
- **Pobieranie danych z Leroy Merlin**: Wbudowany katalog i lookup kodów EAN / numerów referencyjnych (np. 3276007978674, 96058791, 82641234) z automatycznym uzupełnianiem cen, nazw i miniatur.
- **Przenoszenie i zwalnianie miejsc**: Możliwość szybkiego przenoszenia dywanów pomiędzy stojakami oraz zwalniania miejsc.
- **Wyszukiwarka i filtry**: Błyskawiczne filtrowanie po numerze miejsca, statusie (zajęte / wolne) oraz zakresie stojaków (1–10, 11–20, 21–30).
- **Uwierzytelnianie Google**: Tryb tylko do odczytu dla gości oraz pełne uprawnienia edycyjne dla zalogowanych pracowników.

## Uruchomienie lokalne

```bash
npm install
npm run dev
```

Aplikacja uruchamia się na porcie 3000 (`http://localhost:3000`).
