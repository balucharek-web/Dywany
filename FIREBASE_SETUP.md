# Instrukcja Konfiguracji Firebase – Ekspozycja Dywanów Leroy Merlin

> **BARDZO WAŻNE – WŁAŚCICIEL INFRASTRUKTURY:**  
> Cała infrastruktura produkcyjna, baza Firestore, uwierzytelnianie, funkcje Cloud Functions oraz hosting **muszą należeć do konta Google:**  
> **`baluch.arek@gmail.com`**

Zaloguj się do Google/Firebase jako `baluch.arek@gmail.com` i wykonaj poniższe kroki.

---

## KROK 1: Utworzenie Projektu w Firebase Console

1. Otwórz przeglądarkę i przejdź do: **[https://console.firebase.google.com/](https://console.firebase.google.com/)**
2. Upewnij się w prawym górnym rogu, że jesteś zalogowany na konto: **`baluch.arek@gmail.com`**
3. Kliknij **"Dodaj projekt"** (Add project).
4. Nazwij projekt: `ekspozycja-dywanow-lm` (lub inną unikalną nazwą).
5. Wyłącz lub włącz Google Analytics wedle uznania i kliknij **"Utwórz projekt"**.

---

## KROK 2: Włączenie Firebase Authentication

1. W menu bocznym Firebase Console wybierz **Build -> Authentication**.
2. Kliknij **"Rozpocznij"** (Get started).
3. W zakładce **Sign-in method** (Metody logowania) wybierz **Google**:
   - Włącz przełącznik **Włącz** (Enable).
   - Jako e-mail wsparcia projektu wybierz: **`baluch.arek@gmail.com`**.
   - Zapisz (Save).
4. Przejdź do zakładki **Settings -> Authorized domains** (Autoryzowane domeny):
   - Upewnij się, że domena Firebase Hosting (`*.firebaseapp.com`, `*.web.app`) oraz `localhost` są na liście.

---

## KROK 3: Utworzenie Bazy Danych Cloud Firestore

1. W menu bocznym wybierz **Build -> Firestore Database**.
2. Kliknij **"Utwórz bazę danych"** (Create database).
3. Wybierz lokalizację serwera (zalecana dla Polski: `europe-west3` Frankfurt lub `europe-west1` Belgia).
4. Wybierz reguły w trybie produkcyjnym (**Start in production mode**).
5. Kliknij **Utwórz**.

---

## KROK 4: Wdrożenie Reguł Bezpieczeństwa (Security Rules)

Plik `firestore.rules` znajduje się w głównym katalogu tego repozytorium.

Aby wdrożyć reguły z komputera:
```bash
# Zaloguj się w CLI jako baluch.arek@gmail.com
firebase login

# Wybierz utworzony projekt
firebase use ekspozycja-dywanow-lm

# Wdróż reguły bazy Firestore
firebase deploy --only firestore:rules
```

Alternatywnie możesz skopiować zawartość pliku `firestore.rules` bezpośrednio do zakładki **Firestore Database -> Rules** w Firebase Console i kliknąć **Publish**.

---

## KROK 5: Podłączenie Aplikacji Android

1. W Firebase Console kliknij ikonę zębatki obok *Project Overview* -> **Project settings** (Ustawienia projektu).
2. W sekcji *Twoje aplikacje* kliknij ikonę **Android**.
3. Wpisz dane pakietu Androida:
   - **Android package name:** `com.aistudio.carpetdisplay.lmxpkz` (zgodnie z `applicationId` w `app/build.gradle.kts`)
   - **App nickname:** `Ekspozycja Dywanów LM`
   - **Debug signing certificate SHA-1:** (opcjonalnie, wygeneruj poleceniem `keytool -list -v -keystore ~/.android/debug.keystore`)
4. Kliknij **Register app**.
5. Pobierz wygenerowany plik **`google-services.json`**.
6. Skopiuj pobrany plik `google-services.json` do katalogu:
   ```text
   app/google-services.json
   ```
7. Zbuduj aplikację Android:
   ```bash
   gradle :app:assembleRelease
   # lub
   gradle :app:assembleDebug
   ```

---

## KROK 6: Podłączenie Aplikacji Web

1. W Firebase Console w sekcji *Project settings -> Twoje aplikacje* kliknij ikonę **Web** `</>`.
2. Wpisz nazwę: `Ekspozycja Dywanów Web`.
3. Zaznacz opcję: **"Skonfiguruj również Firebase Hosting dla tej aplikacji"**.
4. Skopiuj obiekt `firebaseConfig` i wklej go do pliku `web/app.js`:
   ```javascript
   const firebaseConfig = {
     apiKey: "TWÓJ_PRAWDZIWY_API_KEY",
     authDomain: "twoj-projekt.firebaseapp.com",
     projectId: "twoj-projekt",
     storageBucket: "twoj-projekt.appspot.com",
     messagingSenderId: "...",
     appId: "..."
   };
   ```

---

## KROK 7: Wdrożenie Cloud Functions i Hosting

W głównym katalogu projektu uruchom:
```bash
# Zainstaluj zależności Cloud Functions
cd functions
npm install
cd ..

# Wdróż funkcje backendowe oraz hosting aplikacji Web
firebase deploy --only functions,hosting
```

Po wdrożeniu otrzymasz publiczny adres WWW w domenie `https://twoj-projekt.web.app`.

---

## KROK 8: Inicjalizacja Głównego Administratora (SUPER_ADMIN)

1. Po pierwszym uruchomieniu aplikacji Android lub Web zaloguj się kontem:
   **`baluch.arek@gmail.com`**
2. Aplikacja oraz reguły bezpieczeństwa natychmiast przypiszą temu kontu rolę **SUPER_ADMIN**.
3. Jako SUPER_ADMIN możesz wejść w zakładkę **Konta / Użytkownicy** i dodawać kolejnych pracowników sklepu (np. `marek.nowak@leroymerlin.pl`) nadając im rolę **ADMIN**.
4. Zwykły pracownik (USER) może bez logowania przeszukiwać ekspozycję i skanować kody, ale nie ma praw do modyfikacji bazy.
