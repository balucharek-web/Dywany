import { useState } from 'react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { Download, Smartphone, X, Share, CheckCircle2, MoreVertical } from 'lucide-react';
import { LeroyMerlinLogo } from './LeroyMerlinLogo';

export function PWAInstallButton() {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showInfoModal, setShowInfoModal] = useState(false);

  // If already running strictly inside the standalone PWA window, we don't need to show install
  if (isInstalled) {
    return null;
  }

  const handleClick = async () => {
    if (isInstallable) {
      const success = await install();
      if (!success) {
        setShowInfoModal(true);
      }
    } else {
      setShowInfoModal(true);
    }
  };

  return (
    <>
      <button
        onClick={handleClick}
        className="flex items-center gap-1.5 px-3 py-1.5 text-xs sm:text-sm font-bold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white transition-all shadow-xs active:scale-95 cursor-pointer"
        title="Zainstaluj aplikację na telefonie"
      >
        <Download className="w-3.5 h-3.5" />
        <span>Zainstaluj aplikację</span>
      </button>

      {showInfoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
          <div className="w-full max-w-sm rounded-2xl bg-white p-5 sm:p-6 shadow-2xl flex flex-col gap-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div className="flex items-center gap-2">
                <LeroyMerlinLogo className="h-8" />
                <h3 className="text-base font-bold text-gray-900">
                  Instalacja Dywany 038
                </h3>
              </div>
              <button
                onClick={() => setShowInfoModal(false)}
                className="p-1 text-gray-400 hover:text-gray-700 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {isIOS ? (
              /* iOS Safari instructions */
              <div className="text-xs sm:text-sm text-gray-600 space-y-3 leading-relaxed">
                <p className="font-semibold text-gray-900">
                  Jak zainstalować aplikację na iPhone / iPad:
                </p>
                <p>
                  1. Dotknij przycisku <strong className="text-gray-900 inline-flex items-center gap-1"><Share className="w-3.5 h-3.5" /> Udostępnij</strong> w dolnym pasku przeglądarki Safari.
                </p>
                <p>
                  2. Przewiń w dół i wybierz <strong className="text-gray-900">Do ekranu początkowego</strong>.
                </p>
                <p>
                  3. Dotknij <strong className="text-emerald-700">Dodaj</strong> w prawym górnym rogu.
                </p>
              </div>
            ) : (
              /* Android / Chrome instructions */
              <div className="text-xs sm:text-sm text-gray-700 space-y-3 leading-relaxed">
                <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3 flex items-start gap-2.5">
                  <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                  <div className="text-xs text-emerald-900">
                    <strong className="block font-bold">Aplikacja jest gotowa do uruchomienia!</strong>
                    Jeśli widzisz na dole ekranu komunikat <span className="underline">„Ta aplikacja jest już zainstalowana”</span>, dotknij go lub poszukaj ikony <strong>Dywany 038</strong> bezpośrednio na pulpicie telefonu.
                  </div>
                </div>

                <div className="pt-2 border-t border-gray-100">
                  <span className="font-bold text-gray-900 block mb-1">
                    Jeśli jeszcze nie ma jej na pulpicie:
                  </span>
                  <p>
                    1. Kliknij ikonę menu Chrome (<strong className="inline-flex items-center font-bold text-gray-900"><MoreVertical className="w-3.5 h-3.5" /> trzy kropki</strong> w prawym górnym rogu).
                  </p>
                  <p className="mt-1">
                    2. Wybierz opcję <strong className="text-emerald-700">„Zainstaluj aplikację”</strong> lub <strong className="text-emerald-700">„Dodaj do ekranu głównego”</strong>.
                  </p>
                </div>
              </div>
            )}

            <button
              onClick={() => setShowInfoModal(false)}
              className="mt-2 w-full rounded-xl bg-gray-900 py-2.5 text-xs sm:text-sm font-bold text-white hover:bg-black transition-colors"
            >
              Rozumiem
            </button>
          </div>
        </div>
      )}
    </>
  );
}
