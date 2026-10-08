# Przewodnik Konfiguracji Firebase

## 1. Uwierzytelnianie Google (Google Sign-In)
W konsoli Firebase (Firebase Console):
1. Przejdź do **Authentication** -> **Sign-in method**.
2. Włącz dostawcę **Google**.
3. Dodaj odcisk palca SHA-1 oraz SHA-256 z klucza podpisywania aplikacji Android (KeyStore).

## 2. Cloud Firestore
1. W sekcji **Firestore Database** wybierz utworzenie bazy danych.
2. Skopiuj i wklej zawartość pliku `firestore.rules` do zakładki **Rules**.
3. Kliknij **Publish**.

## 3. Super Admin
Głównym administratorem systemu jest:
`abaluch@leroymerlin.pl`

Logując się tym kontem, użytkownik ma natychmiastowe uprawnienia do:
* Tworzenia i usuwania dowolnych pałąków
* Dodawania i usuwania dywanów
* Dodawania innych administratorów (poprzez wpisanie ich adresu e-mail w panelu administratora)
