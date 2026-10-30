import { useState } from 'react';
import { Download, Smartphone, X, Check } from 'lucide-react';
import { usePWAInstall } from '../hooks/usePWAInstall';

export function PWAInstallButton() {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);
  const [installing, setInstalling] = useState(false);

  // If already installed in standalone mode, hide
  if (isInstalled) {
    return null;
  }

  // Chromium / Android / Desktop flow
  if (isInstallable) {
    return (
      <button
        onClick={async () => {
          setInstalling(true);
          try {
            await install();
          } finally {
            setInstalling(false);
          }
        }}
        disabled={installing}
        className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-xs transition-all active:scale-95 cursor-pointer"
        title="Zainstaluj aplikację LM Dywany na ekranie głównym urządzenia"
      >
        <Download className="w-3.5 h-3.5" />
        <span className="hidden sm:inline">{installing ? 'Instalowanie...' : 'Zainstaluj aplikację'}</span>
        <span className="sm:hidden">Zainstaluj</span>
      </button>
    );
  }

  // iOS Safari flow
  if (isIOS) {
    return (
      <>
        <button
          onClick={() => setShowIOSGuide(true)}
          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs border border-gray-200 transition-colors cursor-pointer"
          title="Instrukcja instalacji na iPhone / iPad"
        >
          <Smartphone className="w-3.5 h-3.5 text-blue-600" />
          <span className="hidden sm:inline">Zainstaluj na iOS</span>
          <span className="sm:hidden">Zainstaluj</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
            <div className="w-full max-w-sm rounded-2xl bg-white p-5 sm:p-6 shadow-2xl border border-gray-100 flex flex-col gap-4 animate-in fade-in zoom-in-95">
              <div className="flex items-center justify-between pb-2 border-b border-gray-100">
                <div className="flex items-center gap-2 font-bold text-gray-900 text-sm">
                  <Smartphone className="w-5 h-5 text-blue-600" />
                  Instalacja na iPhone / iPad
                </div>
                <button
                  onClick={() => setShowIOSGuide(false)}
                  className="p-1 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="text-xs text-gray-700 space-y-3 leading-relaxed">
                <div className="flex items-start gap-2.5 bg-blue-50/70 p-3 rounded-xl border border-blue-100">
                  <span className="w-5 h-5 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center shrink-0 text-[11px]">
                    1
                  </span>
                  <div>
                    W przeglądarce Safari na dole ekranu stuknij ikonę <strong>Udostępnij</strong> (kwadrat ze strzałką w górę ⎋).
                  </div>
                </div>

                <div className="flex items-start gap-2.5 bg-blue-50/70 p-3 rounded-xl border border-blue-100">
                  <span className="w-5 h-5 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center shrink-0 text-[11px]">
                    2
                  </span>
                  <div>
                    Przewiń menu w dół i wybierz opcję <strong>„Do ekranu początkowego” ➕</strong>.
                  </div>
                </div>

                <div className="flex items-start gap-2.5 bg-emerald-50/70 p-3 rounded-xl border border-emerald-100">
                  <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  <div>
                    Aplikacja pojawi się na Twoim pulpicie i będzie otwierać się na pełnym ekranie jak natywny program.
                  </div>
                </div>
              </div>

              <button
                onClick={() => setShowIOSGuide(false)}
                className="w-full py-2.5 rounded-xl bg-gray-900 hover:bg-black text-white font-bold text-xs transition-colors cursor-pointer"
              >
                Zamknij instrukcję
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return null;
}
