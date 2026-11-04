import { useState, useEffect } from 'react';
import { CheckCircle2, Smartphone, X, Download } from 'lucide-react';
import { usePWAInstall } from '../hooks/usePWAInstall';

export function CBAInstallTransitionModal() {
  const { isInstallable, install } = usePWAInstall();
  const [returnUrl, setReturnUrl] = useState<string | null>(null);
  const [isFromCba, setIsFromCba] = useState(false);
  const [showStandaloneWelcome, setShowStandaloneWelcome] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    // 1. Sprawdź czy jesteśmy w trybie zainstalowanej aplikacji (PWA standalone)
    const isStandalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      (window.navigator as unknown as { standalone?: boolean }).standalone === true ||
      document.referrer.includes('android-app://') ||
      window.location.search.includes('utm_source=homescreen');

    // Jeśli jesteśmy w zainstalowanej PWA, ZAWSZE czyścimy flagi CBA, bo użytkownik już JEST w nowej aplikacji!
    if (isStandalone) {
      try {
        localStorage.removeItem('is_from_cba');
        localStorage.removeItem('cba_return_url');
        sessionStorage.removeItem('cba_return_url');
        localStorage.setItem('cba_transition_completed', 'true');

        // Wyczyść ewentualne parametry z paska adresu
        if (window.location.search.includes('from_cba') || window.location.search.includes('return_url')) {
          const cleanUrl = window.location.pathname;
          window.history.replaceState({}, '', cleanUrl);
        }

        const hasSeenTip = localStorage.getItem('lm_saw_shortcut_reminder');
        if (!hasSeenTip) {
          setShowStandaloneWelcome(true);
        }
      } catch {
        // ignore
      }
      return;
    }

    // 2. Jeśli już wcześniej ukończono migrację, nie pokazuj paska
    try {
      if (localStorage.getItem('cba_transition_completed') === 'true') {
        return;
      }
    } catch {
      // ignore
    }

    // 3. Sprawdź parametry URL z CBA (?from_cba=1 & ?return_url=...) w przeglądarce
    let foundReturnUrl: string | null = null;
    let detectedFromCba = false;

    try {
      const urlParams = new URLSearchParams(window.location.search);
      const paramReturnUrl = urlParams.get('return_url');
      const fromCba = urlParams.get('from_cba');

      if (paramReturnUrl) {
        foundReturnUrl = paramReturnUrl;
        detectedFromCba = true;
        sessionStorage.setItem('cba_return_url', paramReturnUrl);
        localStorage.setItem('cba_return_url', paramReturnUrl);
        localStorage.setItem('is_from_cba', '1');
      } else if (fromCba === '1') {
        detectedFromCba = true;
        localStorage.setItem('is_from_cba', '1');
        foundReturnUrl =
          sessionStorage.getItem('cba_return_url') ||
          localStorage.getItem('cba_return_url');
      } else {
        const savedFromCba = localStorage.getItem('is_from_cba') === '1';
        const savedUrl =
          sessionStorage.getItem('cba_return_url') ||
          localStorage.getItem('cba_return_url');

        if (savedFromCba && savedUrl) {
          detectedFromCba = true;
          foundReturnUrl = savedUrl;
        }
      }

      // Wyczyść parametry URL żeby nie zaśmiecały adresu i nie utrwalały się w historii
      if (fromCba || paramReturnUrl) {
        const newUrl = new URL(window.location.href);
        newUrl.searchParams.delete('from_cba');
        newUrl.searchParams.delete('return_url');
        window.history.replaceState({}, '', newUrl.toString());
      }
    } catch {
      // ignore
    }

    if (detectedFromCba) {
      setIsFromCba(true);
    }
    if (foundReturnUrl) {
      setReturnUrl(foundReturnUrl);
    }

    // 4. Obsługa zdarzenia zainstalowania PWA w przeglądarce
    const handleAppInstalled = () => {
      try {
        localStorage.removeItem('is_from_cba');
        localStorage.removeItem('cba_return_url');
        sessionStorage.removeItem('cba_return_url');
        localStorage.setItem('cba_transition_completed', 'true');
      } catch {
        // ignore
      }
      setIsFromCba(false);
      setReturnUrl(null);
      setDismissed(true);
    };

    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const handleCompleteTransition = () => {
    // Użytkownik potwierdził instalację
    try {
      localStorage.removeItem('is_from_cba');
      localStorage.removeItem('cba_return_url');
      sessionStorage.removeItem('cba_return_url');
      localStorage.setItem('cba_transition_completed', 'true');
    } catch {
      // ignore
    }
    setIsFromCba(false);
    setReturnUrl(null);
    setDismissed(true);
  };

  const dismissStandaloneWelcome = () => {
    setShowStandaloneWelcome(false);
    try {
      localStorage.setItem('lm_saw_shortcut_reminder', '1');
    } catch {
      // ignore
    }
  };

  // 1. Toast informacyjny przy pierwszym otwarciu nowej ikony z pulpitu (całkowicie nieblokujący, łatwo zamykany)
  if (showStandaloneWelcome) {
    return (
      <aside
        aria-label="Ważna wskazówka instalacyjna"
        className="fixed top-2 left-2 right-2 sm:left-auto sm:right-4 sm:max-w-md z-50 animate-in fade-in slide-in-from-top-4"
      >
        <div className="bg-white border-2 border-emerald-500 rounded-2xl p-4 shadow-xl flex items-start gap-3">
          <div className="w-9 h-9 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div className="flex-1 min-w-0">
            <h4 className="text-sm font-bold text-gray-900 leading-tight">
              Witaj w LM Dywany 2.0! 🎉
            </h4>
            <p className="text-xs text-gray-600 mt-1 leading-relaxed">
              Jesteś w nowej wersji aplikacji. Możesz teraz spokojnie usunąć stary skrót ze swojego pulpitu, aby nie mylić wersji.
            </p>
            <button
              onClick={dismissStandaloneWelcome}
              className="mt-2.5 px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer"
            >
              Rozumiem, dziękuję
            </button>
          </div>
          <button
            onClick={dismissStandaloneWelcome}
            className="text-gray-400 hover:text-gray-700 p-1 cursor-pointer"
            aria-label="Zamknij"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </aside>
    );
  }

  // 2. Jeśli użytkownik ręcznie zamknął pasek lub nie pochodzi z CBA, nie wyświetlaj nic
  if (dismissed) {
    return null;
  }

  // 3. Pasek asystenta instalacji dla przeglądarki Chrome (gdy strona została otwarta z CBA)
  if (isFromCba || returnUrl) {
    return (
      <aside
        aria-label="Asystent aktualizacji"
        className="bg-emerald-800 text-white px-3 py-3 shadow-lg sticky top-0 z-40 border-b-2 border-emerald-500"
      >
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3 text-center sm:text-left">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-emerald-700/80 border border-emerald-500 flex items-center justify-center shrink-0">
              <Smartphone className="w-5 h-5 text-emerald-200" />
            </div>
            <div>
              <div className="text-xs sm:text-sm font-black flex items-center gap-1.5 justify-center sm:justify-start">
                <span>Aktualizacja z CBA: Zainstaluj wersję 2.0</span>
              </div>
              <p className="text-[11px] text-emerald-100 leading-tight">
                Zainstaluj nową aplikację na pulpicie telefonu, a następnie usuń stary skrót.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            {isInstallable && (
              <button
                onClick={async () => {
                  const success = await install();
                  if (success) {
                    handleCompleteTransition();
                  }
                }}
                className="flex-1 sm:flex-initial px-3.5 py-2 rounded-xl bg-white text-emerald-900 hover:bg-emerald-50 font-black text-xs shadow-sm transition-transform active:scale-95 flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Download className="w-3.5 h-3.5 text-emerald-700" />
                Zainstaluj
              </button>
            )}

            <button
              onClick={handleCompleteTransition}
              className="flex-1 sm:flex-initial px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-emerald-950 font-black text-xs shadow-md transition-all active:scale-95 flex items-center justify-center gap-1.5 cursor-pointer"
              title="Kliknij po zainstalowaniu nowej ikony lub aby zamknąć powiadomienie"
            >
              <CheckCircle2 className="w-4 h-4 text-emerald-950" />
              <span>Gotowe &bull; Zamknij pasek</span>
            </button>

            <button
              onClick={handleCompleteTransition}
              className="p-2 text-emerald-200 hover:text-white rounded-lg hover:bg-emerald-700/50 cursor-pointer"
              title="Zamknij"
              aria-label="Zamknij powiadomienie"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>
    );
  }

  return null;
}
