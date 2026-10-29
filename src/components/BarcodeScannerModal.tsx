import { useEffect, useRef, useState } from 'react';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import { X, Flashlight, AlertCircle, RefreshCw } from 'lucide-react';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (decodedText: string) => void;
}

export function BarcodeScannerModal({ isOpen, onClose, onScanSuccess }: BarcodeScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [isScanningFile, setIsScanningFile] = useState(false);
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const containerId = 'esl-barcode-full-scanner';

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);

    const startScanner = async () => {
      try {
        // Supported formats covering standard retail EAN, ESL (Pricer, SES-imagotag), and Code 128/39/2D
        const html5QrCode = new Html5Qrcode(containerId, {
          formatsToSupport: [
            Html5QrcodeSupportedFormats.EAN_13,
            Html5QrcodeSupportedFormats.EAN_8,
            Html5QrcodeSupportedFormats.CODE_128,
            Html5QrcodeSupportedFormats.CODE_39,
            Html5QrcodeSupportedFormats.UPC_A,
            Html5QrcodeSupportedFormats.UPC_E,
            Html5QrcodeSupportedFormats.DATA_MATRIX,
            Html5QrcodeSupportedFormats.QR_CODE,
            Html5QrcodeSupportedFormats.ITF
          ],
          verbose: false
        });

        scannerRef.current = html5QrCode;

        // Start camera stream optimized for ESL labels (high resolution, autofocus)
        await html5QrCode.start(
          {
            facingMode: 'environment'
          },
          {
            fps: 15,
            qrbox: (viewfinderWidth, viewfinderHeight) => {
              const minDim = Math.min(viewfinderWidth, viewfinderHeight);
              const boxDim = Math.max(220, Math.min(320, Math.floor(minDim * 0.72)));
              return { width: boxDim, height: boxDim };
            },
            aspectRatio: window.innerHeight / window.innerWidth
          },
          (decodedText) => {
            if (isMounted) {
              // Haptic feedback if supported
              if (navigator.vibrate) {
                try {
                  navigator.vibrate(80);
                } catch (_) {}
              }
              onScanSuccess(decodedText.trim());
              onClose();
            }
          },
          () => {
            // frame decode miss (silent)
          }
        );

        // Check if flashlight / torch capability is available on device stream
        try {
          const streamTracks = (html5QrCode as any).getRunningTrackCapabilities?.() || [];
          if (streamTracks && streamTracks.torch) {
            setHasTorch(true);
          }
        } catch (_) {}
      } catch (err: unknown) {
        if (!isMounted) return;
        const errMsg = err instanceof Error ? err.message : String(err);
        console.warn('Camera scanner error:', errMsg);
        setCameraError(
          'Nie udało się uruchomić aparatu (brak uprawnień lub urządzenie nie zezwala na dostęp). Możesz użyć opcji „Skan z zdjęcia” poniżej.'
        );
      }
    };

    const timer = setTimeout(() => {
      startScanner();
    }, 150);

    return () => {
      isMounted = false;
      clearTimeout(timer);
      if (scannerRef.current) {
        scannerRef.current
          .stop()
          .catch(() => {})
          .then(() => {
            try {
              scannerRef.current?.clear();
            } catch (_) {}
          });
      }
    };
  }, [isOpen, onScanSuccess, onClose]);

  if (!isOpen) return null;

  // Toggle camera flashlight
  const toggleTorch = async () => {
    if (!scannerRef.current) return;
    try {
      const nextTorch = !torchOn;
      await (scannerRef.current as any).applyVideoConstraints({
        advanced: [{ torch: nextTorch }]
      });
      setTorchOn(nextTorch);
    } catch (e) {
      console.warn('Torch toggle not supported on this track:', e);
    }
  };

  // Handle image file scanning
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsScanningFile(true);
    setCameraError(null);

    try {
      let fileScanner = scannerRef.current;
      if (!fileScanner) {
        fileScanner = new Html5Qrcode('esl-file-scanner-temp');
      }

      // Try scanning the image with all supported barcode formats
      const decodedText = await fileScanner.scanFile(file, true);
      if (navigator.vibrate) {
        try {
          navigator.vibrate(80);
        } catch (_) {}
      }
      onScanSuccess(decodedText.trim());
      onClose();
    } catch (err) {
      console.warn('Failed to decode barcode from image:', err);
      setCameraError('Nie udało się odczytać kodu kreskowego z tego zdjęcia. Upewnij się, że kod jest ostry i dobrze oświetlony.');
    } finally {
      setIsScanningFile(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col justify-between overflow-hidden select-none">
      {/* Hidden container for temp file scan */}
      <div id="esl-file-scanner-temp" className="hidden" />

      {/* Hidden file input for "Skan z zdjęcia" */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/*"
        className="hidden"
      />

      {/* Camera Video Viewport filling entire screen */}
      <div className="absolute inset-0 z-0 bg-black overflow-hidden flex items-center justify-center">
        <div id={containerId} className="w-full h-full object-cover"></div>
      </div>

      {/* Top Bar with circular Close (X) button matching Screenshot 6 */}
      <div className="relative z-20 flex items-center justify-between p-4 pt-6 sm:p-6 w-full">
        {/* Flashlight toggle if available */}
        {hasTorch ? (
          <button
            onClick={toggleTorch}
            className={`w-11 h-11 rounded-full flex items-center justify-center transition-all ${
              torchOn
                ? 'bg-amber-400 text-gray-950 shadow-lg shadow-amber-400/50'
                : 'bg-black/60 text-white hover:bg-black/80 backdrop-blur-xs'
            }`}
            title="Włącz/wyłącz latarkę"
          >
            <Flashlight className="w-5 h-5" />
          </button>
        ) : (
          <div className="w-11" />
        )}

        {/* Circular Close Button (X) top right */}
        <button
          onClick={onClose}
          className="w-11 h-11 rounded-full bg-black/70 hover:bg-black text-white flex items-center justify-center shadow-lg backdrop-blur-xs transition-transform active:scale-95 cursor-pointer"
          title="Zamknij skaner"
        >
          <X className="w-6 h-6 stroke-[2.5]" />
        </button>
      </div>

      {/* Central ESL Green Viewfinder Box matching Screenshot 6 */}
      <div className="relative z-10 flex-1 flex items-center justify-center pointer-events-none p-4">
        {/* Viewfinder box with rounded corners and neon green border */}
        <div
          className="relative w-[78vw] max-w-[320px] aspect-square rounded-[32px] border-[3.5px] border-[#22c55e] transition-all"
          style={{
            boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.42)'
          }}
        >
          {/* Subtle animated scanline */}
          <div className="absolute inset-x-2 top-0 h-1 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />
        </div>
      </div>

      {/* Bottom Area with "Skan z zdjęcia" Blue Button matching Screenshot 6 */}
      <div className="relative z-20 w-full pb-8 sm:pb-10 pt-4 flex flex-col items-center justify-center gap-3">
        {cameraError && (
          <div className="mx-4 max-w-sm px-4 py-2 bg-black/80 backdrop-blur-md border border-amber-500/50 rounded-xl text-amber-300 text-xs text-center flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>{cameraError}</span>
          </div>
        )}

        {/* Blue button matching Screenshot 6: [📸 Skan z zdjęcia] */}
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={isScanningFile}
          className="py-2.5 px-6 rounded-2xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm shadow-xl flex items-center gap-2 active:scale-95 transition-all cursor-pointer"
        >
          {isScanningFile ? (
            <>
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span>Odczytywanie zdjęcia...</span>
            </>
          ) : (
            <>
              <span className="text-base">📸</span>
              <span>Skan z zdjęcia</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
}
