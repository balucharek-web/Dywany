/**
 * Bezpieczne pobieranie paczki aktualizacyjnej CBA.pl
 * Zapobiega przechwytywaniu pobierania przez aplikację PWA (Service Worker / Android App Links)
 */
export async function downloadCbaPackage(): Promise<boolean> {
  const directRawUrl =
    'https://raw.githubusercontent.com/balucharek-web/Dywany/main/public/paczka_cba_aktualizacja.zip';
  const localUrl = `${import.meta.env.BASE_URL}paczka_cba_aktualizacja.zip`;

  // 1. Pobieranie jako czysty Blob (działa natywnie w przeglądarce bez otwierania nowej podstrony)
  const candidateUrls = [directRawUrl, localUrl];

  for (const targetUrl of candidateUrls) {
    try {
      const response = await fetch(targetUrl, { cache: 'no-cache' });
      if (!response.ok) continue;

      const blob = await response.blob();
      // Odrzuć jeśli pobrano stronę HTML (np. fallback 404 routera PWA)
      if (blob.type.includes('html') || blob.size < 5000) {
        continue;
      }

      // Utwórz dedykowany obiekt Blob z typem application/zip
      const zipBlob = new Blob([blob], { type: 'application/zip' });
      const blobUrl = window.URL.createObjectURL(zipBlob);

      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = blobUrl;
      a.download = 'paczka_cba_aktualizacja.zip';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);

      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 15000);
      return true;
    } catch {
      // Przejdź do kolejnego kandydata
    }
  }

  // 2. Awaryjne pobranie z bezpośredniego URL GitHub Raw (inny host, Android nie przechwyci do PWA)
  try {
    const a = document.createElement('a');
    a.href = directRawUrl;
    a.download = 'paczka_cba_aktualizacja.zip';
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    return true;
  } catch {
    window.location.href = directRawUrl;
    return false;
  }
}
