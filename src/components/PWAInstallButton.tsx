import { useState } from 'react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { Download, Smartphone, X, Share } from 'lucide-react';

export function PWAInstallButton() {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);

  // If already running standalone as installed app, hide prompt
  if (isInstalled) {
    return null;
  }

  // Android / Chrome / Edge install prompt
  if (isInstallable) {
    return (
      <button
        onClick={install}
        className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white transition-all shadow-xs active:scale-95 cursor-pointer"
        title="Zainstaluj aplikację na telefonie lub komputerze"
      >
        <Download className="w-3.5 h-3.5" />
        <span>Zainstaluj aplikację</span>
      </button>
    );
  }

  // iOS Safari flow
  if (isIOS) {
    return (
      <>
        <button
          onClick={() => setShowIOSGuide(true)}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white transition-all shadow-xs active:scale-95 cursor-pointer"
          title="Instrukcja instalacji na iPhone / iPad"
        >
          <Smartphone className="w-3.5 h-3.5" />
          <span>Zainstaluj PWA</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
            <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
              <div className="flex items-center justify-between pb-3 border-b border-gray-100">
                <h3 className="text-base font-bold text-gray-900 flex items-center gap-2">
                  <Smartphone className="w-5 h-5 text-emerald-600" />
                  Instalacja na iPhone / iPad
                </h3>
                <button
                  onClick={() => setShowIOSGuide(false)}
                  className="p-1 text-gray-400 hover:text-gray-700 rounded-lg"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="mt-4 text-xs sm:text-sm text-gray-600 space-y-3 leading-relaxed">
                <p>
                  1. Dotknij przycisku <strong className="text-gray-900 inline-flex items-center gap-1"><Share className="w-3.5 h-3.5" /> Udostępnij</strong> w dolnym pasku Safari.
                </p>
                <p>
                  2. Przewiń w dół i wybierz <strong className="text-gray-900">Do ekranu początkowego</strong>.
                </p>
                <p>
                  3. Dotknij <strong className="text-emerald-700">Dodaj</strong> w prawym górnym rogu.
                </p>
              </div>

              <button
                onClick={() => setShowIOSGuide(false)}
                className="mt-5 w-full rounded-xl bg-gray-900 py-2.5 text-xs sm:text-sm font-bold text-white hover:bg-black transition-colors"
              >
                Rozumiem
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return null;
}
