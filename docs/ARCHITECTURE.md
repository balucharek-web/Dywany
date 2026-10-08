# Architektura Techniczna Systemu

## 1. Przepływ Danych i Synchronizacja Realtime

```
┌───────────────────────────────────────┐
│           UŻYTKOWNIK MOBILNY          │
│    (Skaner aparatem ESL, edycja)      │
└──────────────────┬────────────────────┘
                   │
                   ▼ WriteBatch / SetDoc
┌───────────────────────────────────────┐
│         GOOGLE CLOUD FIRESTORE        │
│    • palki/{palekId}                  │
│    • dywany/{km}                      │
│    • history/{logId}                  │
│    • admins/{email}                   │
└──────────────────┬────────────────────┘
                   │
                   ▼ onSnapshot / snapshots() Flow (< 100ms)
┌───────────────────────────────────────┐
│           UŻYTKOWNIK WEBOWY           │
│   (Ekran stacjonarny punktu dywanów)  │
└───────────────────────────────────────┘
```

## 2. Podział Kolekcji NoSQL

1. `palki/{palekId}`
   * `numer`: Int
   * `slots.A`: `{ km, ean, nazwa, rozmiar, cena, waluta, productUrl, productDataStatus, updatedAt, updatedByEmail }`
   * `slots.B`: `{ km, ean, nazwa, rozmiar, cena, waluta, productUrl, productDataStatus, updatedAt, updatedByEmail }`
   * Uzasadnienie: Odczyt listy wszystkich pałąków pobiera kompletny stan obu miejsc bez dodatkowych zapytań.

2. `dywany/{km}`
   * Identyfikator: 8-cyfrowy kod KM
   * `ean`: String (opcjonalny kod kreskowy EAN)
   * `nazwa`: String (pełna nazwa dywanu)
   * `rozmiar`: String (np. "160 x 230 cm")
   * `cena`: Double (cena w zł)
   * `waluta`: String ("PLN")
   * `productUrl`: String (URL karty produktu w Leroy Merlin)
   * `palekNumer`: Int
   * `miejsce`: String (np. "23A")
   * `slot`: "A" lub "B"
   * `productDataStatus`: "ACTIVE" | "NOT_FOUND" | "MANUAL"
   * `productDataSource`: "leroy_merlin"
   * Uzasadnienie: Natychmiastowe wyszukiwanie O(1) po zeskanowaniu etykiety ESL lub wpisaniu kodu produktu przez pracownika.

3. `history/{logId}`
   * Rejestr zdarzeń audytowych. Reguły zabraniają modyfikacji i usuwania wpisów, gwarantując niezmienność logu operacji.

4. `productPriceHistory/{priceLogId}`
   * Archiwum zmian cen produktów w czasie z audytem wykonawcy.

---

## 3. Automatyczne Pobieranie Danych Produktu Leroy Merlin

```
ANDROID (App) / WEB (SPA)
        │
        ▼ (Google Auth Token)
Firebase Cloud Functions (fetchProductData) / Backend Service
        │
        ├─► [Krok 1] Sprawdzenie pamięci podręcznej Firestore (dywany/{id}) < 24h
        │
        └─► [Krok 2] Oficjalny katalog internetowy Leroy Merlin Polska
                     • Wyszukiwanie: /szukaj?q={id}
                     • Rozpoznanie typu identyfikatora:
                         - 8 cyfr -> numer referencyjny KM
                         - 12-14 cyfr -> kod kreskowy EAN
                     • Parsowanie strukturalnych metadanych Schema.org JSON-LD (Product, Offer)
                     • Fallback na tagi OpenGraph (og:title, product:price:amount)
                     • Ekstrakcja wymiarów dywanu (regex NxN cm)
        │
        ▼ Walidacja danych i status (ACTIVE / NOT_FOUND / ERROR)
Zapis w Firestore (Atomic Batch) -> Natychmiastowa synchronizacja Realtime (Android + Web)
```

### Odporność na Błędy i Gwarancja Ciągłości Pracy:
* **Brak blokad**: Jeżeli strona Leroy Merlin nałoży limit zapytań, produkt nie zostanie odnaleziony lub wystąpi błąd sieciowy, system informuje pracownika czytelnym komunikatem i **ZAWSZE umożliwia ręczne wpisanie danych produktu**.
* **Odporność na zmiany HTML**: Parser opiera się na standardzie Semantic Web (Schema.org JSON-LD), co chroni go przed zmianami layoutu CSS strony.
