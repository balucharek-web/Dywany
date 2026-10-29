import { useEffect, useRef, useState } from 'react';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import { X, Flashlight, AlertCircle, RefreshCw, CheckCircle2 } from 'lucide-react';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (decodedText: string) => void;
}

// Native BarcodeDetector API (built-in hardware ML vision in Google Chrome on Android)
const hasNativeBarcodeDetector = typeof window !== 'undefined' && 'BarcodeDetector' in window;

/**
 * Intelligent barcode classifier for retail ESL (Electronic Shelf Labels):
 * Given any list of barcodes detected in the camera frame at any angle,
 * picks the genuine product code (EAN-13, REF, EAN-8) and ignores the
 * vertical electronic tag hardware ID.
 */
function pickBestProductBarcode(barcodes: any[]): string | null {
  if (!barcodes || barcodes.length === 0) return null;

  const candidates = barcodes.map((b) => {
    const raw = (b.rawValue || (typeof b === 'string' ? b : '')).trim();

    // 1. Check numeric product format (EAN-13, EAN-8, REF, UPC-A)
    const isNumeric = /^\d{7,14}$/.test(raw);
    const isEAN13 = /^\d{13}$/.test(raw);
    const isLMStandard = isEAN13 && (raw.startsWith('590') || raw.startsWith('327') || raw.startsWith('59') || raw.startsWith('32'));

    // 2. Check orientation: horizontal product barcode vs vertical tag barcode
    let isHorizontal = true;
    if (b.cornerPoints && b.cornerPoints.length >= 4) {
      const p0 = b.cornerPoints[0];
      const p1 = b.cornerPoints[1];
      const p3 = b.cornerPoints[3];
      const w = Math.hypot(p1.x - p0.x, p1.y - p0.y);
      const h = Math.hypot(p3.x - p0.x, p3.y - p0.y);
      // If height is notably greater than width, it's the vertical side tag
      if (h > w * 1.1) {
        isHorizontal = false;
      }
    } else if (b.boundingBox) {
      if (b.boundingBox.height > b.boundingBox.width * 1.1) {
        isHorizontal = false;
      }
    }

    // Scoring system:
    let score = 0;
    if (isLMStandard) score += 120;
    else if (isEAN13) score += 100;
    else if (isNumeric) score += 40;

    if (isHorizontal) score += 50;
    else score -= 70; // Penalize vertical orientation (the tag hardware barcode)

    if (!isNumeric) score -= 100; // Penalize alphanumeric strings (MACs / tag IDs)

    return { raw, score, isHorizontal, isNumeric };
  });

  // Sort highest score first
  candidates.sort((a, b) => b.score - a.score);

  const best = candidates[0];
  if (best && best.score > 0 && best.raw) {
    return best.raw;
  }

  return null;
}

export function BarcodeScannerModal({ isOpen, onClose, onScanSuccess }: BarcodeScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [isScanningFile, setIsScanningFile] = useState(false);
  const [scannedFeedback, setScannedFeedback] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const html5QrCodeRef = useRef<Html5Qrcode | null>(null);
  const animFrameIdRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const isScannedRef = useRef(false);

  const containerId = 'esl-barcode-full-scanner';

  const triggerScanSuccess = (code: string) => {
    if (isScannedRef.current) return;
    isScannedRef.current = true;
    setScannedFeedback(code);

    // Immediate haptic feedback
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate([60, 40, 60]);
      } catch (_) {}
    }

    // Brief visual confirmation before closing
    setTimeout(() => {
      onScanSuccess(code.trim());
      onClose();
    }, 200);
  };

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isScannedRef.current = false;
    setScannedFeedback(null);
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);

    const initScanner = async () => {
      // 1. PRIMARY ENGINE: Native Android Google BarcodeDetector (Omnidirectional 360°, Angle-tolerant, 60 FPS)
      if (hasNativeBarcodeDetector) {
        try {
          const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
          const targetFormats = ['ean_13', 'code_128', 'ean_8', 'upc_a', 'code_39', 'itf'].filter((f) =>
            supportedFormats.includes(f)
          );

          const detector = new (window as any).BarcodeDetector({
            formats: targetFormats.length > 0 ? targetFormats : supportedFormats
          });

          // Request high resolution camera with continuous autofocus
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

          // Check torch capability
          const track = stream.getVideoTracks()[0];
          if (track) {
            const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
            if (capabilities.torch) {
              setHasTorch(true);
            }
          }

          if (videoRef.current) {
            videoRef.current.srcObject = stream;
            await videoRef.current.play();

            // Offscreen canvas for dark/low-light boost pass
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d', { willReadFrequently: true });
            canvasRef.current = canvas;

            let frameCount = 0;

            const detectLoop = async () => {
              if (!isMounted || isScannedRef.current) return;

              if (videoRef.current && videoRef.current.readyState >= 2) {
                frameCount++;
                try {
                  // PASS 1: Direct full-screen hardware detection (works at any angle, zero overhead)
                  const rawResults = await detector.detect(videoRef.current);
                  if (rawResults && rawResults.length > 0) {
                    const bestCode = pickBestProductBarcode(rawResults);
                    if (bestCode) {
                      triggerScanSuccess(bestCode);
                      return;
                    }
                  }

                  // PASS 2 (every 3rd frame): Contrast Boost for low-light & e-paper e-ink labels
                  if (frameCount % 3 === 0 && ctx) {
                    const vw = videoRef.current.videoWidth;
                    const vh = videoRef.current.videoHeight;
                    if (vw > 0 && vh > 0) {
                      // Scale down slightly for ultra-fast processing
                      const sw = Math.min(vw, 960);
                      const sh = Math.min(vh, 720);
                      canvas.width = sw;
                      canvas.height = sh;

                      // Strong contrast boost: turns grey e-ink to pure white and barcode lines to pure black
                      ctx.filter = 'contrast(2.2) brightness(1.15) grayscale(1)';
                      ctx.drawImage(videoRef.current, 0, 0, sw, sh);

                      const enhancedResults = await detector.detect(canvas);
                      if (enhancedResults && enhancedResults.length > 0) {
                        const bestCode = pickBestProductBarcode(enhancedResults);
                        if (bestCode) {
                          triggerScanSuccess(bestCode);
                          return;
                        }
                      }
                    }
                  }
                } catch (e) {
                  // ignore frame detection tick
                }
              }

              animFrameIdRef.current = requestAnimationFrame(detectLoop);
            };

            animFrameIdRef.current = requestAnimationFrame(detectLoop);
            return;
          }
        } catch (nativeErr) {
          console.warn('Native BarcodeDetector failed, falling back to Html5Qrcode:', nativeErr);
        }
      }

      // 2. FALLBACK ENGINE: Html5Qrcode
      try {
        const html5QrCode = new Html5Qrcode(containerId, {
          formatsToSupport: [
            Html5QrcodeSupportedFormats.EAN_13,
            Html5QrcodeSupportedFormats.CODE_128,
            Html5QrcodeSupportedFormats.EAN_8,
            Html5QrcodeSupportedFormats.UPC_A,
            Html5QrcodeSupportedFormats.CODE_39
          ],
          verbose: false,
          experimentalFeatures: {
            useBarCodeDetectorIfSupported: true
          }
        });

        html5QrCodeRef.current = html5QrCode;

        await html5QrCode.start(
          { facingMode: 'environment' },
          {
            fps: 25,
            qrbox: (viewfinderWidth, viewfinderHeight) => {
              // Large box covering the whole label
              const minDim = Math.min(viewfinderWidth, viewfinderHeight);
              return {
                width: Math.floor(minDim * 0.88),
                height: Math.floor(minDim * 0.88)
              };
            },
            aspectRatio: window.innerHeight / window.innerWidth
          },
          (decodedText) => {
            if (isMounted) {
              const best = pickBestProductBarcode([{ rawValue: decodedText }]);
              if (best) {
                triggerScanSuccess(best);
              }
            }
          },
          () => {}
        );

        try {
          const trackCaps = (html5QrCode as any).getRunningTrackCapabilities?.() || {};
          if (trackCaps.torch) setHasTorch(true);
        } catch (_) {}
      } catch (err: unknown) {
        if (!isMounted) return;
        const errMsg = err instanceof Error ? err.message : String(err);
        console.warn('Fallback scanner start error:', errMsg);
        setCameraError(
          'Nie udało się uruchomić aparatu. Upewnij się, że przeglądarka ma uprawnienia do kamery lub użyj przycisku „Skan z zdjęcia”.'
        );
      }
    };

    const timer = setTimeout(() => {
      initScanner();
    }, 100);

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

  // Toggle Torch
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

  // Scan from photo file
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsScanningFile(true);
    setCameraError(null);

    try {
      if (hasNativeBarcodeDetector) {
        try {
          const bitmap = await createImageBitmap(file);

          // Contrast enhanced image canvas
          const canvas = document.createElement('canvas');
          canvas.width = bitmap.width;
          canvas.height = bitmap.height;
          const ctx = canvas.getContext('2d');
          if (ctx) {
            ctx.filter = 'contrast(2.2) brightness(1.15) grayscale(1)';
            ctx.drawImage(bitmap, 0, 0);
          }

          const detector = new (window as any).BarcodeDetector();
          const results = await detector.detect(canvas);

          if (results && results.length > 0) {
            const best = pickBestProductBarcode(results);
            if (best) {
              triggerScanSuccess(best);
              return;
            }
          }
        } catch (bitmapErr) {
          console.warn('Native bitmap scan failed:', bitmapErr);
        }
      }

      // Fallback
      let tempScanner = html5QrCodeRef.current;
      if (!tempScanner) {
        tempScanner = new Html5Qrcode('esl-file-scanner-temp');
      }
      const decoded = await tempScanner.scanFile(file, true);
      const best = pickBestProductBarcode([{ rawValue: decoded }]);
      if (best) {
        triggerScanSuccess(best);
      } else {
        setCameraError('Wykryto kod etykiety zamiast kodu produktu. Spróbuj zbliżyć zdjęcie do kodu pod ceną.');
      }
    } catch (err) {
      console.warn('Scan file error:', err);
      setCameraError('Nie udało się odczytać kodu kreskowego z tego zdjęcia. Upewnij się, że kod jest widoczny.');
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

      {/* Video Viewport: renders full screen video */}
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

      {/* Top Bar with circular Close (X) button matching user's original screenshot */}
      <div className="relative z-20 flex items-center justify-between p-4 pt-6 sm:p-6 w-full">
        {/* Flashlight button */}
        {hasTorch ? (
          <button
            onClick={toggleTorch}
            className={`w-12 h-12 rounded-full flex items-center justify-center transition-all ${
              torchOn
                ? 'bg-amber-400 text-gray-950 shadow-xl shadow-amber-400/50 scale-105'
                : 'bg-black/60 text-white hover:bg-black/80 backdrop-blur-xs'
            }`}
            title="Latarka (do słabego oświetlenia)"
          >
            <Flashlight className="w-5 h-5" />
          </button>
        ) : (
          <div className="w-12" />
        )}

        {/* Circular Close Button (X) top right */}
        <button
          onClick={onClose}
          className="w-12 h-12 rounded-full bg-black/70 hover:bg-black text-white flex items-center justify-center shadow-lg backdrop-blur-xs transition-transform active:scale-95 cursor-pointer"
          title="Zamknij skaner"
        >
          <X className="w-6 h-6 stroke-[2.5]" />
        </button>
      </div>

      {/* Central Viewfinder Box matching user's original screenshot (Screenshot 6) */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center pointer-events-none p-4">
        {/* Rounded square frame covering the entire label */}
        <div
          className={`relative w-[82vw] max-w-[340px] aspect-square rounded-[32px] border-[3.5px] transition-all flex items-center justify-center ${
            scannedFeedback
              ? 'border-emerald-400 bg-emerald-500/20'
              : 'border-[#22c55e]'
          }`}
          style={{
            boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.40)'
          }}
        >
          {/* Subtle animated scan glow */}
          <div className="absolute inset-x-4 top-0 h-1 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />

          {/* Scan success indicator */}
          {scannedFeedback && (
            <div className="bg-black/90 text-emerald-400 px-4 py-2 rounded-full flex items-center gap-2 font-bold text-sm shadow-xl">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <span>Odczytano: {scannedFeedback}</span>
            </div>
          )}
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

        {/* Blue button: [📸 Skan z zdjęcia] */}
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={isScanningFile}
          className="py-2.5 px-6 rounded-2xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-sm shadow-xl flex items-center gap-2 active:scale-95 transition-all cursor-pointer"
        >
          {isScanningFile ? (
            <>
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span>Odczytywanie...</span>
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
