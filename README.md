# 🏷️ Ekspozycja Dywanów – System Zarządzania dla Sklepu Leroy Merlin

Kompleksowa aplikacja produkcyjna do zarządzania ekspozycją dywanów na pałąkach/regałach w sklepie Leroy Merlin, działająca w dwóch wersjach:
1. **Android (APK)** – natywna aplikacja w Kotlin + Jetpack Compose z obsługą aparatu (skaner kodów EAN), trybem offline i integracją z profilami służbowymi/osobistymi Google.
2. **Web** – responsywna aplikacja przeglądarkowa (PWA) z obsługą kamery i współdzieloną bazą w czasie rzeczywistym.

---

## 📋 Spis Treści
- [1. Kluczowe Zasady i Architektura](#1-kluczowe-zasady-i-architektura)
- [2. Struktura Projektu](#2-struktura-projektu)
- [3. Instrukcja Konfiguracji Firebase](#3-instrukcja-konfiguracji-firebase)
- [4. Wdrożenie Reguł Bezpieczeństwa (Security Rules)](#4-wdrożenie-reguł-bezpieczeństwa-security-rules)
- [5. Wdrożenie Backend Cloud Functions](#5-wdrożenie-backend-cloud-functions)
- [6. Wdrożenie Wersji Web (Firebase Hosting)](#6-wdrożenie-wersji-web-firebase-hosting)
- [7. Budowanie Aplikacji Android (APK Release)](#7-budowanie-aplikacji-android-apk-release)
- [8. Konfiguracja GitHub & CI/CD](#8-konfiguracja-github--cicd)
- [9. Pierwszy Start i Rola SUPER_ADMIN](#9-pierwszy-start-i-rola-super_admin)
- [10. Testy Jednostkowe](#10-testy-jednostkowe)

---

## 1. Kluczowe Zasady i Architektura

* **Pałąki i Miejsca:** Każdy pałąk mieści dokładnie dwa dywany – **miejsce A** i **miejsce B** (np. `1A`, `1B`, `23A`, `23B`).
* **Format Kodów EAN:** Kody EAN **nigdy nie są obcinane** do 8 cyfr. Pełny ciąg kreskowy (np. 13 cyfr `5901234567890`) jest zapisywany w całości.
* **Separacja EAN i Numeru Systemowego LM:** Numer systemowy Leroy Merlin (zazwyczaj 8 cyfr) oraz kod kreskowy EAN to dwa odrębne pola w bazie.
* **Rozdzielenie Produktu od Ekspozycji:** Usunięcie dywanu z ekspozycji zwalnia miejsce na pałąku, ale zachowuje produkt w katalogu.
* **Cena Lokalna vs Cena Online:**
  - `onlinePrice`: cena pobrana ze strony Leroy Merlin.
  - `localPrice`: cena nadpisana w sklepie.
  - `localPriceOverride`: flaga włączenia ceny lokalnej. Automatyczna synchronizacja **nigdy** nie nadpisuje ceny lokalnej, gdy ta flaga jest aktywna.
* **Synchronizacja Real-time:** Zmiana lokalizacji (np. `23A → 15B`) jest natychmiast widoczna na wszystkich telefonach i przeglądarkach bez odświeżania.
* **Uprawnienia i Role:**
  - `USER`: publiczne wyszukiwanie, przeglądanie, skanowanie kodów (bez konieczności logowania).
  - `ADMIN`: logowanie przez Google, dodawanie dywanów, edycja cen lokalnych, przenoszenie, zamiana miejsc, usuwanie z ekspozycji, zarządzanie pałąkami.
  - `SUPER_ADMIN`: pełne prawa administracyjne oraz dodawanie administratorów i zarządzanie uprawnieniami.
* **Główny Administrator:** Konto `baluch.arek@gmail.com` jest automatycznie inicjalizowane z rolą `SUPER_ADMIN` i zabezpieczone po stronie reguł Firestore. System uniemożliwia pozostawienie aplikacji bez co najmniej jednego `SUPER_ADMIN`.

---

## 2. Struktura Projektu

```text
├── app/                        # Natywna aplikacja Android (Kotlin + Jetpack Compose)
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── model/         # Modele: User, Product, Pole, DisplayAssignment, AuditLog
│   │   │   ├── repository/    # RugDisplayRepository (Firestore, transakcje, audyt)
│   │   │   └── util/          # Obsługa błędów Firestore (handleFirestoreError)
│   │   └── ui/
│   │       ├── MainViewModel.kt
│   │       ├── screens/       # HomeScreen (Wyszukiwarka, Siatka pałąków, Auth)
│   │       ├── scanner/       # BarcodeScannerDialog (CameraX + ML Kit)
│   │       ├── dialogs/       # Dialogi: AddRug, MoveRug, SwapRug, ProductEdit, ManagePoles, UserManagement, AuditHistory
│   │       └── theme/         # Kolorystyka i komponenty Material Design 3
│   └── src/test/java/         # Testy jednostkowe i Robolectric
├── web/                        # Wersja Web (HTML5, CSS3, JS, Firebase SDK)
│   ├── index.html             # Interfejs przeglądarkowy
│   ├── styles.css             # Style responsywne
│   └── app.js                 # Realtime Firestore, Google Sign-In, HTML5 Barcode Scanner
├── functions/                  # Backend Firebase Cloud Functions (Node.js / TypeScript)
│   ├── src/index.ts           # Callable functions, scraper, scheduled sync
│   └── src/leroyMerlinService.ts # Moduł pobierania danych ze strony Leroy Merlin
├── firestore.rules             # Twarde reguły bezpieczeństwa Firestore
├── firebase.json               # Konfiguracja Firestore, Functions, Hosting, Emulatorów
├── firebase-blueprint.json     # Schemat bazy danych IR
└── README.md
```

---

## 3. Instrukcja Konfiguracji Firebase

Właścicielem projektu Google / Firebase ma być konto: **baluch.arek@gmail.com**.

1. Zaloguj się w konsoli [Firebase Console](https://console.firebase.google.com/) za pomocą konta `baluch.arek@gmail.com`.
2. Kliknij **Add project** (Dodaj projekt) i nadaj nazwę (np. `leroy-ekspozycja-dywanow`).
3. Wyłącz lub włącz Google Analytics według preferencji i zatwierdź utworzenie projektu.
4. **Włącz Authentication:**
   - W menu bocznym przejdź do **Build** → **Authentication** → **Get started**.
   - W zakładce **Sign-in method** wybierz **Google** i włącz dostawcę.
   - Wpisz publiczną nazwę projektu oraz email wsparcia (`baluch.arek@gmail.com`).
5. **Utwórz bazę Cloud Firestore:**
   - Przejdź do **Build** → **Firestore Database** → **Create database**.
   - Wybierz lokalizację bazy (np. `europe-west1` lub `europe-west3`).
   - Wybierz tryb produkcyjny (**Start in production mode**).
6. **Dodaj aplikację Android:**
   - Kliknij ikonę Androida na stronie głównej projektu.
   - Pakiet aplikacji: `com.aistudio.rugdisplay.ekspozycja`.
   - Wpisz odcisk certyfikatu SHA-1 i SHA-256 wygenerowany z Twojego keystore.
   - Pobierz wygenerowany plik `google-services.json` i umieść go w katalogu `app/google-services.json`.
7. **Dodaj aplikację Web:**
   - Kliknij ikonę Web (`</>`) na stronie głównej projektu.
   - Zarejestruj aplikację i zaznacz **Also set up Firebase Hosting for this app**.
   - Skopiuj obiekt `firebaseConfig` i wklej go na początku pliku `web/app.js`.

---

## 4. Wdrożenie Reguł Bezpieczeństwa (Security Rules)

Reguły zawarte w pliku `firestore.rules` zabezpieczają bazę danych:
- Publiczny odczyt katalogu produktów, pałąków i aktualnych przypisań dla wszystkich pracowników i klientów (bez konieczności logowania).
- Zapis, edycja i usuwanie dozwolone wyłącznie dla użytkowników z rolą `ADMIN` lub `SUPER_ADMIN`.
- Zarządzanie rolami i administratorami zastrzeżone ściśle dla `SUPER_ADMIN`.
- Audit logs są niezmienne (`update` i `delete` zabronione).

Aby wdrożyć reguły na żywo do projektu Firebase:
```bash
firebase deploy --only firestore:rules
```

---

## 5. Wdrożenie Backend Cloud Functions

Backend odpowiada za automatyczne pobieranie danych ze strony Leroy Merlin i okresową synchronizację:

1. Wejdź do katalogu `functions`:
   ```bash
   cd functions
   npm install
   npm run build
   ```
2. Wdróż funkcje do chmury Firebase:
   ```bash
   firebase deploy --only functions
   ```

Dostępne funkcje w chmurze:
- `getProductByEAN({ ean })` – bezpieczne pobieranie danych produktu po kodzie kreskowym EAN.
- `getProductByLMNumber({ lmNumber })` – wyszukiwanie po 8-cyfrowym numerze Leroy Merlin.
- `scheduledProductUpdate` – zadanie cron działające co 24h (lub co 6h/12h w zależności od konfiguracji), sprawdzające ceny na stronie z zachowaniem ręcznie wpisanych cen lokalnych.
- `transferSuperAdminRole` – atomowa zmiana głównego administratora.

---

## 6. Wdrożenie Wersji Web (Firebase Hosting)

Wersja webowa korzysta z tej samej bazy Firestore w czasie rzeczywistym.

1. Zbuduj i sprawdź pliki w katalogu `web/`.
2. Wdróż hosting:
   ```bash
   firebase deploy --only hosting
   ```
3. Aplikacja webowa będzie natychmiast dostępna pod adresem:
   `https://<twój-projekt-id>.web.app`

---

## 7. Budowanie Aplikacji Android (APK Release)

Aby zbudować gotowy pakiet instalacyjny APK na telefony:

1. W terminalu uruchom zadanie Gradle:
   ```bash
   gradle :app:assembleRelease
   ```
   *(lub `gradle :app:assembleDebug` dla wersji testowej)*.
2. Zbudowany plik APK znajdziesz w:
   `app/build/outputs/apk/release/app-release.apk` (lub `.../debug/app-debug.apk`).
3. Prześlij plik na telefon z systemem Android lub zainstaluj przez MDM / profil służbowy.

---

## 8. Konfiguracja GitHub & CI/CD

Projekt jest w pełni przygotowany do umieszczenia w repozytorium GitHub:

```bash
git init
git add .
git commit -m "Initial commit - Complete Leroy Merlin Rug Display System"
git branch -M main
git remote add origin https://github.com/<twoj-login>/<repozytorium>.git
git push -u origin main
```

**Bezpieczeństwo na GitHubie:**
- Plik `.gitignore` chroni przed wysłaniem kluczy prywatnych, plików `debug.keystore` oraz logów.
- W repozytorium nie umieszczaj żadnych haseł ani prywatnych kluczy konta usługowego (Service Account).

---

## 9. Pierwszy Start i Rola SUPER_ADMIN

1. Uruchom aplikację na Androidzie lub w przeglądarce.
2. Kliknij **Zaloguj przez Google** i wybierz konto:
   `baluch.arek@gmail.com`
3. Aplikacja automatycznie rozpozna ten adres i nada mu uprawnienia **SUPER_ADMIN**.
4. Jako Główny Administrator masz dostęp do przycisku **⚙ Tryb administratora / Panel edycji**, z poziomu którego możesz:
   - Dodać kolejnych administratorów sklepu (`+ Dodaj administratora` wpisując ich adresy Google).
   - Dodać pałąki ekspozycyjne (system automatycznie zasugeruje kolejne numery).
   - Dodać dywany na ekspozycję ze skanowaniem EAN.
   - Zmieniać ceny lokalne z blokadą nadpisywania online.
   - Bezpiecznie przekazać funkcję Głównego Administratora innemu administratorowi.

---

## 10. Testy Jednostkowe

Projekt zawiera kompletny zestaw testów weryfikujących logikę biznesową:
- Weryfikacja nieobcinania kodów EAN.
- Odrębność EAN od numeru systemowego Leroy Merlin.
- Poprawność algorytmu nadpisywania cen lokalnych (`effectivePrice`).
- Izolacja uprawnień ról `USER`, `ADMIN` i `SUPER_ADMIN`.
- Inwariant bezpieczeństwa uniemożliwiający pozostawienie systemu bez `SUPER_ADMIN`.
- Walidacja reguł pałąka (wyłącznie 2 miejsca: A i B).

Uruchomienie testów:
```bash
# Testy reguł Firestore w JavaScript (sub-second):
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

# Testy Android Robolectric i JUnit:
gradle :app:testDebugUnitTest
```
