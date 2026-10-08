# Leroy Merlin – System Ekspozycji Dywanów

[![Zbuduj aplikację Android (APK)](https://github.com/balucharek-web/Dywany/actions/workflows/build-apk.yml/badge.svg)](https://github.com/balucharek-web/Dywany/actions/workflows/build-apk.yml)
[![Pobierz APK](https://img.shields.io/badge/Pobierz%20APK-Najnowsza%20Wersja-008037?logo=android&logoColor=white)](https://github.com/balucharek-web/Dywany/releases/latest/download/app-debug.apk)

### 📲 [Bezpośrednie pobieranie pliku instalacyjnego APK (Direct Download)](https://github.com/balucharek-web/Dywany/releases/latest/download/app-debug.apk)
Wszystkie wydania i artefakty: [Wydania GitHub (Releases)](https://github.com/balucharek-web/Dywany/releases)  
Aplikacja Webowa (GitHub Pages): [https://balucharek-web.github.io/Dywany/](https://balucharek-web.github.io/Dywany/)

---

Profesjonalny, produkcyjny system do zarządzania ekspozycją dywanów na pałąkach wahadłowych w sklepach sieci **Leroy Merlin**.

System składa się z dwóch zsynchronizowanych w czasie rzeczywistym aplikacji:
1. **Aplikacja mobilna Android** (Kotlin, Jetpack Compose, CameraX + ML Kit do skanowania kodów kreskowych z etykiet ESL).
2. **Aplikacja webowa desktopowa** (React, Tailwind CSS, split-screen z podglądem siatki ekspozycji i panelem wyszukiwania).

Obie aplikacje korzystają z tej samej bazy **Google Cloud Firestore** oraz uwierzytelniania **Google Sign-In**.

---

## 1. Główna zasada działania

W dziale dywanów w sklepie stacjonarnym znajdują się uchwyty/pałąki wahadłowe. Każdy pałąk posiada dokładnie dwa ponumerowane miejsca ekspozycyjne:
* **Miejsce A**
* **Miejsce B**

Lokalizację dywanu w sklepie opisuje symbol złożony z numeru pałąka i litery miejsca (np. `1A`, `1B`, `23A`, `23B`).

Każdy produkt posiada **8-cyfrowy numer KM** (np. `45657894`) odczytywany bezpośrednio z etykiety ESL (Electronic Shelf Label) lub z kodu kreskowego.

---

## 2. Funkcje systemu

* **Logowanie Google**: Bezpieczne uwierzytelnianie przez konto Google pracownika (Jetpack Credential Manager na Androidzie, Google Identity na Webie).
* **Role użytkowników (RBAC)**:
  * **SUPER_ADMIN** (`abaluch@leroymerlin.pl`): pełny dostęp do systemu, wyłączne prawo do dodawania i usuwania administratorów. Ochrona przed usunięciem w Security Rules.
  * **ADMIN**: dodawanie, usuwanie i edycja pałąków; przypisywanie, przenoszenie i zamiana dywanów miejscami.
  * **USER** (zwykły pracownik): podgląd ekspozycji, wyszukiwanie, skanowanie kodów kreskowych.
* **Inteligentna wyszukiwarka (3 w 1)**:
  1. *Numer KM* (np. `45657894`): natychmiast lokalizuje dywan i podświetla odpowiedni pałąk.
  2. *Miejsce* (np. `23A`, `1B`): bezpośredni filtr do wybranego miejsca.
  3. *Numer pałąka lub nazwa*: wyszukiwanie tekstowe.
* **Skaner kodów kreskowych aparatem (Android)**:
  * Obsługa CameraX + Google ML Kit Barcode Scanning.
  * Błyskawiczny odczyt kodów kreskowych z etykiet ESL i automatyczne wyszukanie dywanu na pałąku.
  * Przełącznik latarki (flashlight) oraz animowany celownik laserowy.
* **Zamiana i przenoszenie (Swap / Move)**:
  * Szybka zamiana miejsc A ↔ B na tym samym pałąku.
  * Przeniesienie dywanu na inne dowolne miejsce w sklepie (np. `23A` → `15B`).
  * Jeśli miejsce docelowe jest zajęte, system atomowo zamienia oba dywany miejscami bez konfliktów!
* **Synchronizacja w czasie rzeczywistym**:
  * Zmiana wykonana na telefonie pracownika pojawia się na ekranie komputera w ciągu kilkudziesięciu milisekund bez konieczności odświeżania strony.
* **Rejestr audytowy (Historia zmian)**:
  * Niezmienna historia operacji (`ADD`, `REMOVE`, `MOVE`, `SWAP`, `CREATE_PALEK`, `DELETE_PALEK`) z danymi wykonawcy i czasem.
* **Odporność na błędy i offline**:
  * Wbudowany cache Firestore pozwala na przeglądanie ekspozycji nawet przy chwilowym braku łączności Wi-Fi w hali sklepu.

---

## 3. Struktura repozytorium

```
leroy-merlin-system-ekspozycji-dywanow/
├── README.md                          # Główna dokumentacja
├── firestore.rules                    # Reguły bezpieczeństwa bazy danych
├── firebase.json                      # Konfiguracja Firestore i emulatorów
├── firebase-blueprint.json            # Schemat danych systemu
│
├── /app/                              # Aplikacja Android (Kotlin / Compose)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── model/ (Palek, RugSlot, Dywan, HistoryLog, UserRole)
│       │   │   └── repository/ (RugRepository)
│       │   ├── ui/
│       │   │   ├── components/ (PalekCard, AssignRugDialog, SwapOrMoveDialog...)
│       │   │   ├── scanner/ (BarcodeScannerView z CameraX + ML Kit)
│       │   │   ├── screens/ (HomeScreen, AuthScreen)
│       │   │   ├── theme/ (Color, Theme z barwami Leroy Merlin)
│       │   │   └── viewmodel/ (RugViewModel)
│       └── res/ (drawable, mipmap, values)
│
├── /web/                              # Aplikacja Webowa (Desktop / Przeglądarka)
│   ├── index.html                     # Kompletna aplikacja SPA (React + Tailwind)
│   └── package.json
│
└── /docs/                             # Dokumentacja techniczna
    ├── ARCHITECTURE.md
    └── FIREBASE_SETUP.md
```

---

## 4. Konfiguracja Firebase i Bezpieczeństwo

### Reguły Firestore (`firestore.rules`)
Reguły egzekwują zasadę **Zero-Trust**:
* Użytkownik bez logowania nie ma dostępu do bazy.
* Użytkownik `USER` może jedynie odczytywać dane.
* Tylko użytkownicy ze statusem `ADMIN` lub `SUPER_ADMIN` mogą modyfikować pałąki i dywany.
* Super Admin (`abaluch@leroymerlin.pl`) jest chroniony przed usunięciem w samej regule bazy danych:
  ```javascript
  match /admins/{adminEmail} {
    allow read: if isSignedIn();
    allow create, update: if isSuperAdmin();
    allow delete: if isSuperAdmin() && adminEmail != 'abaluch@leroymerlin.pl';
  }
  ```
* Walidacja numeru KM: każda próba zapisu dywanu o numerze innym niż dokładnie 8 cyfr jest odrzucana na poziomie reguł bazy (`data.km.matches('^[0-9]{8}$')`).

---

## 5. Uruchomienie aplikacji

### Wersja Android:
1. Otwórz projekt w **Android Studio**.
2. Upewnij się, że plik `app/google-services.json` znajduje się w katalogu `app/`.
3. Wybierz urządzenie lub emulator z Androidem (min. SDK 24, zalecane Android 12+).
4. Kliknij **Run** (`gradle assembleDebug`).

### Wersja Web:
Aplikacja webowa to gotowy plik SPA korzystający z bibliotek CDN i modularnego Firebase SDK v10.
1. Wejdź do katalogu `web/`:
   ```bash
   cd web
   ```
2. Uruchom dowolny lokalny serwer HTTP, np.:
   ```bash
   npx serve .
   ```
   lub otwórz plik `web/index.html` bezpośrednio w przeglądarce Chrome, Edge lub Firefox.

---

## 6. Rozwiązywanie konfliktów jednoczesnej edycji

Gdy dwóch pracowników próbuje w tej samej chwili zmienić to samo miejsce:
* Wszelkie operacje przenoszenia i zamiany miejsc realizowane są atomowo (`writeBatch` / transakcje Firestore).
* Dane aktualizowane są jednocześnie w dokumencie pałąka (`palki/{palekId}`) oraz w indeksie produktu (`dywany/{km}`).
* Brak ryzyka wystąpienia stanu niejednoznacznego lub osieroconych rekordów.
