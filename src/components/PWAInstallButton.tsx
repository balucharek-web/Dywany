import { useState, useEffect } from 'react';
import { Download, Smartphone, X, Share, CheckCircle2, MoreVertical, ExternalLink } from 'lucide-react';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

export function PWAInstallButton({ variant = 'default' }: { variant?: 'default' | 'card' }) {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [showGuide, setShowGuide] = useState(false);
  const [isIOS, setIsIOS] = useState(false);
  const [isInAppBrowser, setIsInAppBrowser] = useState(false);

  useEffect(() => {
    // Detect iOS
    const ua = navigator.userAgent || '';
    const isIOSDevice = /iPad|iPhone|iPod/.test(ua);
    setIsIOS(isIOSDevice);

    // Detect In-App browser or Webview (Messenger, Gmail, Instagram, TikTok, Custom Tab)
    const isInApp = /FBAN|FBAV|Instagram|Line|Twitter|MicroMessenger|GSA|Snapchat|BytedanceWebview/i.test(ua);
    setIsInAppBrowser(isInApp);

    // Listen to standard beforeinstallprompt
    const handlePrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
    };

    window.addEventListener('beforeinstallprompt', handlePrompt);
    return () => window.removeEventListener('beforeinstallprompt', handlePrompt);
  }, []);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      try {
        await deferredPrompt.prompt();
        const choice = await deferredPrompt.userChoice;
        if (choice.outcome === 'accepted') {
          setDeferredPrompt(null);
          return;
        }
      } catch (err) {
        console.warn('Install prompt error:', err);
      }
    }
    // If native prompt wasn't triggered or was dismissed, show guided modal
    setShowGuide(true);
  };

  return (
    <>
      {variant === 'card' ? (
        <div className="w-full bg-emerald-50 border-2 border-emerald-500/40 rounded-2xl p-4 flex flex-col items-center text-center gap-3 shadow-xs">
          <div className="flex items-center gap-2 text-emerald-900 font-black text-sm">
            <Download className="w-5 h-5 text-emerald-600 animate-bounce" />
            Zainstaluj aplikację na telefonie
          </div>
          <p className="text-xs text-emerald-800/90 max-w-xs leading-relaxed">
            Dodaj ikonę <strong className="font-bold text-emerald-950">LM Dywany</strong> do ekranu głównego telefonu, aby mieć natychmiastowy dostęp bez wpisywania adresu.
          </p>
          <button
            onClick={handleInstallClick}
            className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs sm:text-sm font-bold shadow-md active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer"
          >
            <Smartphone className="w-4 h-4" />
            {deferredPrompt ? 'Zainstaluj teraz na telefonie' : 'Instrukcja instalacji / Dodaj do ekranu'}
          </button>
        </div>
      ) : (
        <button
          onClick={handleInstallClick}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs sm:text-sm font-bold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white transition-all shadow-xs active:scale-95 cursor-pointer"
          title="Zainstaluj aplikację na telefonie"
        >
          <Download className="w-3.5 h-3.5" />
          <span>Zainstaluj aplikację</span>
        </button>
      )}

      {/* Guided Modal */}
      {showGuide && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-xs">
          <div className="w-full max-w-md rounded-3xl bg-white p-5 sm:p-6 shadow-2xl flex flex-col gap-4 text-gray-900">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-2">
                <LeroyMerlinLogo className="h-8" />
                <h3 className="text-base sm:text-lg font-black text-gray-900">
                  Instalacja LM Dywany
                </h3>
              </div>
              <button
                onClick={() => setShowGuide(false)}
                className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-full transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* In-App Browser Warning */}
            <div className="bg-amber-50 border border-amber-300 rounded-2xl p-3.5 flex items-start gap-3 text-xs text-amber-950">
              <ExternalLink className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
              <div>
                <strong className="block font-bold text-amber-900 mb-0.5">
                  Ważne: Otwórz bezpośrednio w aplikacji Chrome!
                </strong>
                Jeśli na samej górze ekranu widzisz krzyżyk <span className="font-bold">[✕]</span>, oznacza to podgląd wewnątrz aplikacji, który blokuje instalowanie plików.
                <p className="mt-1 font-semibold text-gray-900">
                  Dotknij <strong>trzy kropki (⋮)</strong> w prawym górnym rogu ekranu i kliknij <strong>„Otwórz w Chrome”</strong> (lub „Otwórz w przeglądarce”).
                </p>
              </div>
            </div>

            {isIOS ? (
              /* iOS Safari instructions */
              <div className="bg-gray-50 rounded-2xl p-4 border border-gray-200 text-xs sm:text-sm text-gray-700 space-y-2.5">
                <p className="font-bold text-gray-900">
                  Instrukcja na iPhone / iPad (Safari):
                </p>
                <p className="flex items-center gap-2">
                  1. Dotknij przycisku <strong className="inline-flex items-center gap-1 font-bold"><Share className="w-4 h-4 text-blue-600" /> Udostępnij</strong> w dolnym menu.
                </p>
                <p>
                  2. Przewiń w dół i wybierz opcję <strong>„Do ekranu początkowego”</strong>.
                </p>
                <p>
                  3. Kliknij <strong>„Dodaj”</strong> w prawym górnym rogu.
                </p>
              </div>
            ) : (
              /* Android Chrome instructions */
              <div className="bg-gray-50 rounded-2xl p-4 border border-gray-200 text-xs sm:text-sm text-gray-700 space-y-2.5">
                <p className="font-bold text-gray-900">
                  Instrukcja na telefon z systemem Android (Google Chrome):
                </p>
                <p>
                  1. W przeglądarce Chrome dotknij <strong>trzy pionowe kropki (⋮)</strong> w prawym górnym rogu.
                </p>
                <p>
                  2. Wybierz z menu pozycję <strong className="text-emerald-700">„Zainstaluj aplikację”</strong> (lub <strong>„Dodaj do ekranu głównego”</strong>).
                </p>
                <p>
                  3. Potwierdź klikając <strong className="text-emerald-700">„Zainstaluj”</strong>. Na Twoim pulpicie pojawi się ikona <strong>LM Dywany</strong>!
                </p>
              </div>
            )}

            <button
              onClick={() => setShowGuide(false)}
              className="mt-2 w-full rounded-xl bg-blue-600 py-3 text-xs sm:text-sm font-bold text-white hover:bg-blue-700 transition-colors shadow-sm"
            >
              Rozumiem, przechodzę do Chrome
            </button>
          </div>
        </div>
      )}
    </>
  );
}
