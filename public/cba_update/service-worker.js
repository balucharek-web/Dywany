// service-worker.js — CBA.pl -> Migracja do GitHub Pages v2.0
// Czyści wszystkie stare pamięci podręczne (lm-cache-v3 itp.) i wyrejestrowuje Service Workera

self.addEventListener('install', event => {
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(
    (async () => {
      try {
        // 1. Wyczyść wszelkie stare pamięci podręczne z CBA
        const cacheNames = await caches.keys();
        await Promise.all(cacheNames.map(name => caches.delete(name)));
        console.log('[CBA Migration] Wyszyszczono stare pamięci podręczne.');
      } catch (e) {
        console.warn('[CBA Migration] Błąd czyszczenia cache:', e);
      }

      // 2. Przejmij kontrolę
      await self.clients.claim();

      // 3. Wyrejestruj Service Worker z domeny CBA, aby nie blokował kolejnych wejść
      try {
        await self.registration.unregister();
        console.log('[CBA Migration] Service Worker wyrejestrowany z CBA.');
      } catch (e) {
        console.warn('[CBA Migration] Błąd wyrejestrowania:', e);
      }
    })()
  );
});

// Zawsze pobieraj świeży plik index (brak agresywnego cachowania)
self.addEventListener('fetch', event => {
  event.respondWith(
    fetch(event.request, { cache: 'no-cache' }).catch(() => caches.match(event.request))
  );
});
