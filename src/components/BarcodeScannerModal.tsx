import React, { useState, useEffect, useRef } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { X, QrCode, Search, VideoOff } from 'lucide-react';

interface BarcodeScannerModalProps {
  onScanCode: (code: string) => void;
  onClose: () => void;
}

export const BarcodeScannerModal: React.FC<BarcodeScannerModalProps> = ({
  onScanCode,
  onClose
}) => {
  const [manualCode, setManualCode] = useState('');
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const scannerElementId = 'html5-barcode-scanner-view';

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Initialize camera scanner
  useEffect(() => {
    let isMounted = true;
    const scanner = new Html5Qrcode(scannerElementId);
    scannerRef.current = scanner;

    const startCamera = async () => {
      try {
        const config = {
          fps: 12,
          qrbox: { width: 260, height: 160 },
          aspectRatio: 1.333
        };

        const handleSuccess = (decodedText: string) => {
          if (isMounted && decodedText) {
            scanner.stop().catch(() => {}).finally(() => {
              onScanCode(decodedText);
            });
          }
        };

        // Try environment (back) camera first
        try {
          await scanner.start(
            { facingMode: 'environment' },
            config,
            handleSuccess,
            () => {}
          );
          if (isMounted) {
            setCameraActive(true);
            setCameraError(null);
          }
          return;
        } catch (envErr) {
          console.warn("Back camera direct start failed, trying any available camera:", envErr);
        }

        // Fallback: enumerate cameras
        const cameras = await Html5Qrcode.getCameras();
        if (cameras && cameras.length > 0) {
          const backCam = cameras.find(c =>
            c.label.toLowerCase().includes('back') ||
            c.label.toLowerCase().includes('rear') ||
            c.label.toLowerCase().includes('otoczenia')
          ) || cameras[cameras.length - 1];

          await scanner.start(
            backCam.id,
            config,
            handleSuccess,
            () => {}
          );
          if (isMounted) {
            setCameraActive(true);
            setCameraError(null);
          }
          return;
        }

        // Fallback: user facing camera
        await scanner.start(
          { facingMode: 'user' },
          config,
          handleSuccess,
          () => {}
        );
        if (isMounted) {
          setCameraActive(true);
          setCameraError(null);
        }
      } catch (err: any) {
        if (isMounted) {
          console.error("Camera start error:", err);
          setCameraError(
            err?.message?.includes('NotAllowedError') || err?.message?.includes('Permission')
              ? 'Wymagane zezwolenie na dostęp do aparatu w przeglądarce.'
              : 'Aparat niedostępny w tej przeglądarce lub urządzeniu. Użyj pola poniżej.'
          );
        }
      }
    };

    startCamera();

    return () => {
      isMounted = false;
      if (scannerRef.current) {
        scannerRef.current.stop().catch(() => {}).finally(() => {
          try {
            scannerRef.current?.clear();
          } catch (e) {}
        });
      }
    };
  }, [onScanCode]);

  const handleManualSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const code = manualCode.trim();
    if (!code) return;
    onScanCode(code);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 border border-slate-100 my-8">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-2">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center font-bold">
              <QrCode className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-sm">
                Skaner ESL / EAN
              </h3>
              <p className="text-[11px] text-slate-400">
                Wyszukiwanie miejsca, dywanu i kodu etykiety
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1 rounded-lg cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Manual Code Input Form */}
        <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200/80 space-y-2.5">
          <label className="block text-xs font-semibold text-slate-700">
            Wpisz kod ręcznie lub podłącz czytnik kodów:
          </label>
          <form onSubmit={handleManualSubmit} className="flex gap-2">
            <input
              type="text"
              autoFocus
              value={manualCode}
              onChange={(e) => setManualCode(e.target.value)}
              placeholder="np. 3276007978674, 96058791, 1a, ESL-1A"
              className="flex-1 px-3 py-2 rounded-lg border border-slate-200 text-xs bg-white focus:outline-hidden focus:ring-2 focus:ring-blue-600"
            />
            <button
              type="submit"
              disabled={!manualCode.trim()}
              className="px-3.5 py-2 rounded-lg bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs flex items-center space-x-1 transition cursor-pointer disabled:opacity-50"
            >
              <Search className="w-3.5 h-3.5" />
              <span>Szukaj</span>
            </button>
          </form>

          {/* Quick test buttons */}
          <div className="pt-1">
            <div className="text-[10px] font-semibold text-slate-500 mb-1">
              Szybki test (1 kliknięcie):
            </div>
            <div className="flex flex-wrap gap-1.5">
              <button
                type="button"
                onClick={() => onScanCode('3276007978674')}
                className="px-2 py-0.5 rounded-full bg-blue-100 hover:bg-blue-200 text-blue-800 text-[10px] font-bold cursor-pointer"
              >
                📌 Plakat: 3276007978674
              </button>
              <button
                type="button"
                onClick={() => onScanCode('96058791')}
                className="px-2 py-0.5 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 text-[10px] font-semibold cursor-pointer"
              >
                Ref: 96058791
              </button>
              <button
                type="button"
                onClick={() => onScanCode('1a')}
                className="px-2 py-0.5 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 text-[10px] font-semibold cursor-pointer"
              >
                Miejsce 1a
              </button>
              <button
                type="button"
                onClick={() => onScanCode('ESL-1A')}
                className="px-2 py-0.5 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 text-[10px] font-semibold cursor-pointer"
              >
                Etykieta ESL-1A
              </button>
              <button
                type="button"
                onClick={() => onScanCode('5901234567890')}
                className="px-2 py-0.5 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 text-[10px] font-semibold cursor-pointer"
              >
                Dywan Agnella
              </button>
            </div>
          </div>
        </div>

        {/* Live Camera Viewfinder Box */}
        <div className="relative rounded-xl overflow-hidden bg-slate-900 border border-slate-800 min-h-[220px] flex items-center justify-center">
          <div id={scannerElementId} className="w-full"></div>

          {cameraError && (
            <div className="absolute inset-0 bg-slate-900/90 flex flex-col items-center justify-center p-6 text-center text-white space-y-2">
              <VideoOff className="w-8 h-8 text-amber-400" />
              <div className="text-xs font-semibold">{cameraError}</div>
              <p className="text-[11px] text-slate-400">
                Wpisz kod ręcznie w polu powyżej lub wybierz z szybkich podpowiedzi.
              </p>
            </div>
          )}

          {!cameraError && cameraActive && (
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 bg-black/70 px-3 py-1 rounded-full text-[10px] text-white font-medium pointer-events-none">
              Skieruj aparat na kod kreskowy lub etykietę ESL
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
