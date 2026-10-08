# Leroy Merlin – System Ekspozycji Dywanów

Aplikacja mobilna (Android) oraz webowa (Web SPA) dedykowana obsłudze i zarządzaniu ekspozycją dywanów w sklepach Leroy Merlin na pałąkach wystawowych (Pałąk 1, 2, ... N; sloty A i B).

---

## 1. Główne funkcjonalności

- **System Pałąków i Miejsc**:
  - Dwu-slotowa struktura każdego pałąka: **A** i **B**.
  - Błyskawiczne przypisywanie dywanu do pałąka i slotu.
  - Zdejmowanie dywanu ze slotu (zwolnienie miejsca).
  - Zamiana dywanów miejscami na pałąku (**A ↔ B**) jednym kliknięciem.
  - Przenoszenie dywanu między dowolnymi pałąkami i slotami.
  - Wyszukiwanie w czasie rzeczywistym po:
    - Numerze pałąka i slocie (np. `12A`, `3B`),
    - 8-cyfrowym numerze Leroy Merlin,
    - Pełnym kodzie EAN (13 cyfr),
    - Nazwie handlowej i wymiarach dywanu.

- **Skaner kodów kreskowych (CameraX + Google ML Kit)**:
  - Skanowanie kodów kreskowych w czasie rzeczywistym.
  - **Pełne zachowanie kodu EAN**: kod EAN przechowywany jest jako String w pełnej długości (np. `5901234567890`), bez obcinania.
  - Osobne rozpoznawanie 8-cyfrowego numeru LM.

- **Integracja z danymi produktów Leroy Merlin**:
  - Pobieranie i synchronizacja nazwy, rozmiaru, ceny, linku URL do produktu oraz statusu dostępności.
  - Rejestracja historii zmian cen.

- **Synchronizacja Realtime (Firebase Firestore)**:
  - Każda zmiana na Androidzie natychmiast odzwierciedla się w aplikacji Web oraz na innych urządzeniach.

- **Role i Uprawnienia**:
  - `SUPER_ADMIN`: **abaluch@leroymerlin.pl** (pełny dostęp, zarządzanie uprawnieniami).
  - `ADMIN`: edycja katalogu, zarządzanie pałąkami, zatwierdzanie zmian.
  - `USER`: odczyt, wyszukiwanie, skanowanie, podgląd lokalizacji.

- **Rejestr Historii Operacji (`HistoryLog`)**:
  - Zapis typu akcji (`ASSIGN`, `REMOVE`, `SWAP`, `MOVE`), daty i godziny, e-maila pracownika oraz szczegółów zmian.

---

## 2. Architektura i Technologie

- **Android**:
  - Język: **Kotlin**
  - UI: **Jetpack Compose (Material 3)**
  - Architektura: **MVVM** (Model-View-ViewModel) + Clean Architecture
  - Skaner: CameraX + Google ML Kit Barcode Scanning
  - Autoryzacja: Jetpack Credential Manager (Google Sign-In) z obsługą profilu służbowego Android Work Profile
  - Baza danych: Firebase Firestore Realtime SDK

- **Web**:
  - Responsywny Dashboard (HTML5, Tailwind CSS, JavaScript)
  - Firebase JS SDK v10/v11 (`onSnapshot` Realtime)
  - Zgodność z przeglądarkami: Google Chrome, Microsoft Edge, Mozilla Firefox

- **Backend & Cloud Functions**:
  - Node.js (Firebase Functions v5, Firebase Admin v12)
  - Bezpieczne reguły Firestore Security Rules (ABAC/RBAC)

---

## 3. Uruchomienie i Konfiguracja

### Android:
1. Otwórz projekt w **Android Studio**.
2. Upewnij się, że w `app/` znajduje się poprawny plik `google-services.json` z Twojego projektu Firebase.
3. Zbuduj i uruchom aplikację:
   ```bash
   gradle assembleDebug
   ```

### Web:
1. Otwórz plik `web/index.html` bezpośrednio w przeglądarce lub uruchom lokalny serwer HTTP:
   ```bash
   npx serve web
   ```

### Cloud Functions:
```bash
cd functions
npm install
firebase deploy --only functions
```
