# Ekspozycja Dywanów – Leroy Merlin

Kompletny, produkcyjny system do zarządzania ekspozycją dywanów na pałąkach w sklepie Leroy Merlin.
Projekt zawiera natywną aplikację **Android (Kotlin + Jetpack Compose)**, aplikację **Web (HTML5 + JS + PWA)** oraz backend **Firebase (Cloud Firestore, Authentication, Cloud Functions)**.

---

## 📌 Najważniejsze Założenia i Architektura

1. **Właściciel Infrastruktury:**  
   Wszelkie zasoby produkcyjne (Firebase, Firestore, Cloud Functions, Hosting) należą docelowo do konta:  
   **`baluch.arek@gmail.com`** (Główny SUPER_ADMIN). Szczegóły w pliku [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

2. **Ekspozycja na Pałąkach (Miejsca A i B):**  
   Każdy pałąk w sklepie ma dokładnie dwa miejsca: **A** i **B** (np. `1A`, `1B`, `23A`, `23B`).  
   System nie pozwala na przypisanie dwóch produktów do tego samego miejsca ani usunięcie pałąka zawierającego dywany.

3. **Zachowanie Pełnego Kodu EAN (Krytyczne):**  
   Skaner kodów kreskowych oraz baza danych **nigdy nie obcinają odczytanego kodu EAN** (np. 13-cyfrowy kod `5901234567890` pozostaje nienaruszony).  
   Numer systemowy Leroy Merlin (zazwyczaj 8-cyfrowy, np. `82451923`) jest przechowywany jako całkowicie oddzielny identyfikator (`lmSystemNumber`).

4. **Ceny Internetowe vs Ceny Lokalne:**  
   Każdy dywan posiada cenę ze strony internetowej (`onlinePrice`) oraz cenę lokalną w sklepie (`localPrice`).  
   Gdy administrator włączy `localPriceOverride = true`, automatyczna synchronizacja ze strony Leroy Merlin **nie nadpisuje ceny lokalnej**.

5. **Wyszukiwanie Wielokryterialne:**  
   Główna wyszukiwarka na stronie startowej umożliwia wyszukiwanie po:
   - Skanowaniu aparatem (aparat telefonu lub kamera internetowa)
   - Pełnym kodzie EAN
   - Numerze systemowym Leroy Merlin
   - Miejscu ekspozycji (np. wpisanie `23A` od razu wyświetla dywan z Pałąka 23, miejsce A)
   - Nazwie dywanu

6. **Role i Bezpieczeństwo:**
   - **USER (Gość/Pracownik):** Może wyszukiwać, skanować i przeglądać ekspozycję bez konieczności logowania. Brak uprawnień do edycji.
   - **ADMIN:** Może dodawać dywany, przenosić, zamieniać miejsca (atomowo), edytować ceny lokalne i zarządzać pałąkami.
   - **SUPER_ADMIN:** Wszystko co ADMIN oraz zarządzanie administratorami i bezpieczne przekazywanie uprawnień (system blokuje usunięcie ostatniego SUPER_ADMIN).
   - Logowanie przez standardowy mechanizm Google Sign-In (obsługuje profile prywatne oraz profil służbowy na urządzeniach z systemem Android Enterprise).

7. **Synchronizacja w Czasie Rzeczywistym i Cache Offline:**  
   Aplikacja korzysta z nasłuchu `onSnapshot` w Firestore. Każda zmiana (np. przeniesienie `23A → 15B`) jest widoczna natychmiast na wszystkich urządzeniach. Przy braku sieci aktywny jest lokalny cache Firestore.

---

## 🛠️ Budowanie Aplikacji Android (APK)

Projekt został skonfigurowany w standardzie Gradle z Kotlin DSL.

### 1. Pobranie i konfiguracja projektu
Upewnij się, że w katalogu `app/` znajduje się plik `google-services.json` pobrany z Twojego projektu Firebase (patrz [FIREBASE_SETUP.md](FIREBASE_SETUP.md)). W trybie deweloperskim bez tego pliku aplikacja automatycznie uruchamia się w bezpiecznym trybie lokalnym / demonstracyjnym.

### 2. Budowanie pliku APK Debug:
```bash
gradle :app:assembleDebug
```
Wygenerowany plik APK znajdziesz w katalogu:  
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Budowanie produkcyjnego pliku APK Release:
```bash
gradle :app:assembleRelease
```
Wygenerowany plik APK znajdziesz w katalogu:  
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 🌐 Uruchomienie i Wdrożenie Wersji Web

Aplikacja Web znajduje się w katalogu `web/`.

### 1. Testowanie lokalne:
Możesz uruchomić prosty serwer HTTP:
```bash
# Python:
python3 -m http.server 8080 --directory web

# lub Node.js (npx serve):
npx serve web
```
Otwórz w przeglądarce: `http://localhost:8080`.

### 2. Wdrożenie na Firebase Hosting:
```bash
firebase deploy --only hosting
```
Aplikacja będzie dostępna pod adresem: `https://ekspozycja-dywanow-lm.web.app`.

---

## ⚡ Wdrożenie Cloud Functions

W katalogu `functions/` znajduje się backend realizujący:
- Pobieranie danych dywanu z bazy/katalogu (`getProductByEAN`, `getProductByLMNumber`)
- Zadanie cykliczne (`scheduledUpdateProducts`) synchronizujące ceny online (co 6h, 12h, 24h, 48h, 7 dni) z ochroną cen lokalnych.

Wdrożenie:
```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

---

## 📂 Struktura Repozytorium

```text
├── app/                               # Moduł aplikacji Android (Kotlin + Jetpack Compose)
│   ├── src/main/java/com/example/
│   │   ├── auth/                      # Google Sign-In via Credential Manager
│   │   ├── model/                     # Modele danych (Product, Pole, Assignment, AuditLog, Roles)
│   │   ├── repository/                # Firestore Repository i In-Memory Cache
│   │   ├── ui/                        # Ekrany Compose (Home, Display, History, Users, Settings)
│   │   └── util/                      # Parser miejsc (np. 23A) i walidator EAN
│   └── src/main/res/                  # Ikony adaptacyjne, kolory LM, stringi
├── web/                               # Responsywna wersja przeglądarkowa (HTML5/CSS/JS)
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── functions/                         # Firebase Cloud Functions (Node.js)
│   ├── index.js
│   ├── lmCatalogFetcher.js
│   └── package.json
├── firestore.rules                    # Reguły bezpieczeństwa Firestore
├── FIREBASE_SETUP.md                  # Instrukcja krok po kroku dla baluch.arek@gmail.com
├── .env.example                       # Przykładowe zmienne środowiskowe
├── .gitignore                         # Ignorowanie kluczy, buildów i sekretów
└── README.md
```

---

## 🔒 Bezpieczeństwo i Przygotowanie pod GitHub

- Plik `.gitignore` chroni przed przypadkowym wysłaniem do repozytorium kluczy prywatnych, keystore, tokenów API oraz plików sesji deweloperskich.
- Wszelkie uprawnienia są weryfikowane po stronie reguł Firestore (`firestore.rules`), dzięki czemu zwykły użytkownik nie ma możliwości zmodyfikowania bazy nawet po dekompilacji aplikacji.
