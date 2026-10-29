import { useEffect, useRef, useState } from 'react';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import { X, Flashlight, AlertCircle, RefreshCw, ZoomIn } from 'lucide-react';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (decodedText: string) => void;
}

// Check native BarcodeDetector support
const hasNativeBarcodeDetector = typeof window !== 'undefined' && 'BarcodeDetector' in window;

export function BarcodeScannerModal({ isOpen, onClose, onScanSuccess }: BarcodeScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [hasZoom, setHasZoom] = useState(false);
  const [maxZoom, setMaxZoom] = useState(2);
  const [isScanningFile, setIsScanningFile] = useState(false);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const html5QrCodeRef = useRef<Html5Qrcode | null>(null);
  const animFrameIdRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const isScannedRef = useRef(false);

  const containerId = 'esl-barcode-full-scanner';

  const triggerScanSuccess = (code: string) => {
    if (isScannedRef.current) return;
    isScannedRef.current = true;

    // Haptic vibration feedback on scan
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate([60, 40, 60]);
      } catch (_) {}
    }

    onScanSuccess(code.trim());
    onClose();
  };

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isScannedRef.current = false;
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);
    setHasZoom(false);
    setZoomLevel(1);

    const initScanner = async () => {
      // 1. Try modern native BarcodeDetector API first (standard in Chrome on Android, optimized for retail ESL e-ink barcodes)
      if (hasNativeBarcodeDetector) {
        try {
          const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
          const targetFormats = [
            'ean_13',
            'ean_8',
            'code_128',
            'code_39',
            'code_93',
            'codabar',
            'itf',
            'upc_a',
            'upc_e',
            'data_matrix',
            'qr_code'
          ].filter((f) => supportedFormats.includes(f));

          const barcodeDetector = new (window as any).BarcodeDetector({
            formats: targetFormats.length > 0 ? targetFormats : supportedFormats
          });

          // Request camera with high resolution and continuous autofocus for sharp ESL barcode recognition
          const stream = await navigator.mediaDevices.getUserMedia({
            video: {
              facingMode: { ideal: 'environment' },
              width: { min: 1280, ideal: 1920 },
              height: { min: 720, ideal: 1080 }
            },
            audio: false
          });

          if (!isMounted) {
            stream.getTracks().forEach((t) => t.stop());
            return;
          }

          streamRef.current = stream;

          // Check capabilities (zoom, torch)
          const track = stream.getVideoTracks()[0];
          if (track) {
            const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
            if (capabilities.torch) {
              setHasTorch(true);
            }
            if (capabilities.zoom) {
              setHasZoom(true);
              setMaxZoom(Math.min(capabilities.zoom.max || 3, 3));
            }
          }

          if (videoRef.current) {
            videoRef.current.srcObject = stream;
            await videoRef.current.play();

            // Continuous detection loop
            const detectFrame = async () => {
              if (!isMounted || isScannedRef.current) return;

              if (videoRef.current && videoRef.current.readyState >= 2) {
                try {
                  const barcodes = await barcodeDetector.detect(videoRef.current);
                  if (barcodes && barcodes.length > 0) {
                    const rawVal = barcodes[0].rawValue;
                    if (rawVal) {
                      triggerScanSuccess(rawVal);
                      return;
                    }
                  }
                } catch (e) {
                  // silent frame parse error
                }
              }

              animFrameIdRef.current = requestAnimationFrame(detectFrame);
            };

            animFrameIdRef.current = requestAnimationFrame(detectFrame);
            return;
          }
        } catch (nativeErr) {
          console.warn('Native BarcodeDetector init failed, falling back to Html5Qrcode:', nativeErr);
        }
      }

      // 2. Fallback to Html5Qrcode (ZXing core)
      try {
        const html5QrCode = new Html5Qrcode(containerId, {
          formatsToSupport: [
            Html5QrcodeSupportedFormats.EAN_13,
            Html5QrcodeSupportedFormats.EAN_8,
            Html5QrcodeSupportedFormats.CODE_128,
            Html5QrcodeSupportedFormats.CODE_39,
            Html5QrcodeSupportedFormats.CODE_93,
            Html5QrcodeSupportedFormats.CODABAR,
            Html5QrcodeSupportedFormats.ITF,
            Html5QrcodeSupportedFormats.UPC_A,
            Html5QrcodeSupportedFormats.UPC_E,
            Html5QrcodeSupportedFormats.DATA_MATRIX,
            Html5QrcodeSupportedFormats.QR_CODE
          ],
          verbose: false
        });

        html5QrCodeRef.current = html5QrCode;

        await html5QrCode.start(
          { facingMode: 'environment' },
          {
            fps: 15,
            qrbox: (viewfinderWidth, viewfinderHeight) => {
              const minDim = Math.min(viewfinderWidth, viewfinderHeight);
              const boxDim = Math.max(220, Math.min(340, Math.floor(minDim * 0.75)));
              return { width: boxDim, height: boxDim };
            },
            aspectRatio: window.innerHeight / window.innerWidth
          },
          (decodedText) => {
            if (isMounted) {
              triggerScanSuccess(decodedText);
            }
          },
          () => {}
        );

        try {
          const trackCaps = (html5QrCode as any).getRunningTrackCapabilities?.() || {};
          if (trackCaps.torch) setHasTorch(true);
          if (trackCaps.zoom) {
            setHasZoom(true);
            setMaxZoom(Math.min(trackCaps.zoom.max || 3, 3));
          }
        } catch (_) {}
      } catch (err: unknown) {
        if (!isMounted) return;
        const errMsg = err instanceof Error ? err.message : String(err);
        console.warn('Fallback scanner start error:', errMsg);
        setCameraError(
          'Nie udało się uruchomić aparatu (brak uprawnień lub urządzenie nie zezwala na dostęp). Użyj przycisku „Skan z zdjęcia” poniżej.'
        );
      }
    };

    const timer = setTimeout(() => {
      initScanner();
    }, 150);

    return () => {
      isMounted = false;
      clearTimeout(timer);
      if (animFrameIdRef.current) {
        cancelAnimationFrame(animFrameIdRef.current);
      }
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
        streamRef.current = null;
      }
      if (html5QrCodeRef.current) {
        html5QrCodeRef.current
          .stop()
          .catch(() => {})
          .then(() => {
            try {
              html5QrCodeRef.current?.clear();
            } catch (_) {}
          });
      }
    };
  }, [isOpen]);

  if (!isOpen) return null;

  // Toggle Torch (latarka)
  const toggleTorch = async () => {
    const next = !torchOn;
    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ torch: next }]
          });
          setTorchOn(next);
          return;
        }
      }
      if (html5QrCodeRef.current) {
        await (html5QrCodeRef.current as any).applyVideoConstraints({
          advanced: [{ torch: next }]
        });
        setTorchOn(next);
      }
    } catch (e) {
      console.warn('Torch failed:', e);
    }
  };

  // Toggle Zoom (1x <-> 2x)
  const toggleZoom = async () => {
    const nextZoom = zoomLevel === 1 ? Math.min(2.0, maxZoom) : 1;
    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ zoom: nextZoom }]
          });
          setZoomLevel(nextZoom);
          return;
        }
      }
      if (html5QrCodeRef.current) {
        await (html5QrCodeRef.current as any).applyVideoConstraints({
          advanced: [{ zoom: nextZoom }]
        });
        setZoomLevel(nextZoom);
      }
    } catch (e) {
      console.warn('Zoom failed:', e);
    }
  };

  // Scan from photo / ESL label image
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsScanningFile(true);
    setCameraError(null);

    try {
      // First try native detector with ImageBitmap
      if (hasNativeBarcodeDetector) {
        try {
          const bitmap = await createImageBitmap(file);
          const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
          const detector = new (window as any).BarcodeDetector({
            formats: supportedFormats
          });
          const results = await detector.detect(bitmap);
          if (results && results.length > 0 && results[0].rawValue) {
            triggerScanSuccess(results[0].rawValue);
            return;
          }
        } catch (bitmapErr) {
          console.warn('Native image detection failed, falling back to html5QrCode:', bitmapErr);
        }
      }

      // Fallback to Html5Qrcode.scanFile
      let tempScanner = html5QrCodeRef.current;
      if (!tempScanner) {
        tempScanner = new Html5Qrcode('esl-file-scanner-temp');
      }

      const decoded = await tempScanner.scanFile(file, true);
      triggerScanSuccess(decoded);
    } catch (err) {
      console.warn('Scan file error:', err);
      setCameraError('Nie udało się odczytać kodu kreskowego z tego zdjęcia. Upewnij się, że kod na etykiecie ESL jest ostry i czytelny.');
    } finally {
      setIsScanningFile(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col justify-between overflow-hidden select-none">
      {/* Hidden container for image scan fallback */}
      <div id="esl-file-scanner-temp" className="hidden" />

      {/* Hidden file input for "Skan z zdjęcia" */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/*"
        capture="environment"
        className="hidden"
      />

      {/* Video Viewport: renders native video or html5-qrcode container */}
      <div className="absolute inset-0 z-0 bg-black overflow-hidden flex items-center justify-center">
        {hasNativeBarcodeDetector ? (
          <video
            ref={videoRef}
            playsInline
            muted
            className="w-full h-full object-cover"
          />
        ) : (
          <div id={containerId} className="w-full h-full object-cover"></div>
        )}
      </div>

      {/* Top Bar with circular Close (X) button matching Screenshot */}
      <div className="relative z-20 flex items-center justify-between p-4 pt-6 sm:p-6 w-full">
        <div className="flex items-center gap-2">
          {/* Flashlight button */}
          {hasTorch && (
            <button
              onClick={toggleTorch}
              className={`w-11 h-11 rounded-full flex items-center justify-center transition-all ${
                torchOn
                  ? 'bg-amber-400 text-gray-950 shadow-lg shadow-amber-400/50'
                  : 'bg-black/60 text-white hover:bg-black/80 backdrop-blur-xs'
              }`}
              title="Latarka (do ciemniejszych półek i ekranów ESL)"
            >
              <Flashlight className="w-5 h-5" />
            </button>
          )}

          {/* Zoom button (1x / 2x for small ESL barcodes) */}
          {hasZoom && (
            <button
              onClick={toggleZoom}
              className="w-11 h-11 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center backdrop-blur-xs text-xs font-black shadow-lg"
              title="Powiększenie (2x do małych etykiet ESL)"
            >
              {zoomLevel === 1 ? '1×' : '2×'}
            </button>
          )}
        </div>

        {/* Circular Close Button (X) top right */}
        <button
          onClick={onClose}
          className="w-11 h-11 rounded-full bg-black/70 hover:bg-black text-white flex items-center justify-center shadow-lg backdrop-blur-xs transition-transform active:scale-95 cursor-pointer"
          title="Zamknij skaner"
        >
          <X className="w-6 h-6 stroke-[2.5]" />
        </button>
      </div>

      {/* Central ESL Green Viewfinder Box matching Screenshot */}
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

      {/* Bottom Area with "Skan z zdjęcia" Blue Button matching Screenshot */}
      <div className="relative z-20 w-full pb-8 sm:pb-10 pt-4 flex flex-col items-center justify-center gap-3">
        {cameraError && (
          <div className="mx-4 max-w-sm px-4 py-2 bg-black/80 backdrop-blur-md border border-amber-500/50 rounded-xl text-amber-300 text-xs text-center flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>{cameraError}</span>
          </div>
        )}

        {/* Blue button matching Screenshot: [📸 Skan z zdjęcia] */}
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={isScanningFile}
          className="py-2.5 px-6 rounded-2xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm shadow-xl flex items-center gap-2 active:scale-95 transition-all cursor-pointer"
        >
          {isScanningFile ? (
            <>
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span>Odczytywanie etykiety ESL...</span>
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
