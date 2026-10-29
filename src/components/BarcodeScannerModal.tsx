import { useEffect, useRef, useState } from 'react';
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode';
import { X, Flashlight, AlertCircle, RefreshCw, ZoomIn, CheckCircle2 } from 'lucide-react';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (decodedText: string) => void;
}

// Native BarcodeDetector API (built-in hardware vision in Google Chrome on Android)
const hasNativeBarcodeDetector = typeof window !== 'undefined' && 'BarcodeDetector' in window;

/**
 * Filter specifically tailored for Leroy Merlin ESL labels:
 * Rejects the vertical tag barcode on the right edge and keeps only
 * the horizontal product EAN/REF code.
 */
function isValidProductBarcode(
  code: string,
  cornerPoints?: Array<{ x: number; y: number }>,
  boundingBox?: DOMRectReadOnly
): boolean {
  const clean = code.trim();

  // 1. REJECT VERTICAL BARCODES (The vertical ESL hardware tag on the right edge)
  if (cornerPoints && cornerPoints.length >= 4) {
    const p0 = cornerPoints[0];
    const p1 = cornerPoints[1];
    const p3 = cornerPoints[3];
    const width = Math.hypot(p1.x - p0.x, p1.y - p0.y);
    const height = Math.hypot(p3.x - p0.x, p3.y - p0.y);

    // If barcode is taller than wide, it is 100% the vertical ESL tag barcode!
    if (height > width * 1.05) {
      return false;
    }
  } else if (boundingBox) {
    if (boundingBox.height > boundingBox.width * 1.05) {
      return false;
    }
  }

  // 2. REJECT NON-PRODUCT STRINGS (Tag serials, MACs, letters)
  // Product barcodes in LM are EAN-13 (13 digits), REF (8 digits), EAN-8 (8 digits), or UPC-A (12 digits)
  const isNumeric = /^\d{7,14}$/.test(clean);
  if (!isNumeric) {
    return false;
  }

  return true;
}

export function BarcodeScannerModal({ isOpen, onClose, onScanSuccess }: BarcodeScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1.5);
  const [hasZoom, setHasZoom] = useState(false);
  const [maxZoom, setMaxZoom] = useState(2.5);
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

    // Haptic vibration feedback
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate([60, 40, 60]);
      } catch (_) {}
    }

    // Brief delay to show green confirmation
    setTimeout(() => {
      onScanSuccess(code.trim());
      onClose();
    }, 250);
  };

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isScannedRef.current = false;
    setScannedFeedback(null);
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);
    setHasZoom(false);
    setZoomLevel(1.5);

    const initScanner = async () => {
      // 1. Try Native BarcodeDetector (Chrome Android Hardware Vision)
      if (hasNativeBarcodeDetector) {
        try {
          const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
          // Priority to retail 1D formats for faster processing
          const retailFormats = ['ean_13', 'code_128', 'ean_8', 'upc_a', 'code_39'];
          const activeFormats = retailFormats.filter((f) => supportedFormats.includes(f));

          const barcodeDetector = new (window as any).BarcodeDetector({
            formats: activeFormats.length > 0 ? activeFormats : supportedFormats
          });

          // Request camera in high-def with continuous focus
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

          // Check track capabilities (zoom, torch)
          const track = stream.getVideoTracks()[0];
          if (track) {
            const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
            if (capabilities.torch) {
              setHasTorch(true);
            }
            if (capabilities.zoom) {
              setHasZoom(true);
              const maxZ = Math.min(capabilities.zoom.max || 3, 3);
              const defaultZ = Math.min(1.5, maxZ);
              setMaxZoom(maxZ);
              setZoomLevel(defaultZ);

              // Apply 1.5x zoom by default so user doesn't have to hold phone too close (macro blur fix)
              try {
                await (track as any).applyConstraints({
                  advanced: [{ zoom: defaultZ }]
                });
              } catch (_) {}
            }
          }

          if (videoRef.current) {
            videoRef.current.srcObject = stream;
            await videoRef.current.play();

            // Prepare offscreen canvas for ROI contrast enhancement
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d', { willReadFrequently: true });
            canvasRef.current = canvas;

            let lastScanTime = 0;

            const detectLoop = async (now: number) => {
              if (!isMounted || isScannedRef.current) return;

              // Scan every 50ms (~20 FPS) for instant lock
              if (now - lastScanTime > 50 && videoRef.current && videoRef.current.readyState >= 2) {
                lastScanTime = now;
                const vw = videoRef.current.videoWidth;
                const vh = videoRef.current.videoHeight;

                if (vw > 0 && vh > 0) {
                  try {
                    // Extract central horizontal ROI (matching the horizontal green box)
                    // ESL labels have horizontal barcodes. Scanning only the central 70% width and 35% height
                    // guarantees we NEVER capture the vertical tag on the right edge!
                    const cropW = Math.floor(vw * 0.75);
                    const cropH = Math.floor(vh * 0.35);
                    const cropX = Math.floor((vw - cropW) / 2);
                    const cropY = Math.floor((vh - cropH) / 2);

                    canvas.width = cropW;
                    canvas.height = cropH;

                    if (ctx) {
                      // Apply E-Ink Contrast Enhancement:
                      // Boost contrast to turn gray e-paper background to pure white and barcode lines to pure black
                      ctx.filter = 'contrast(1.8) brightness(1.1) grayscale(1)';
                      ctx.drawImage(videoRef.current, cropX, cropY, cropW, cropH, 0, 0, cropW, cropH);

                      // Detect in enhanced ROI canvas
                      const results = await barcodeDetector.detect(canvas);
                      if (results && results.length > 0) {
                        for (const item of results) {
                          const val = item.rawValue;
                          if (val && isValidProductBarcode(val, item.cornerPoints, item.boundingBox)) {
                            triggerScanSuccess(val);
                            return;
                          }
                        }
                      }
                    }

                    // Fallback pass on full video frame if ROI didn't catch
                    const fullResults = await barcodeDetector.detect(videoRef.current);
                    if (fullResults && fullResults.length > 0) {
                      for (const item of fullResults) {
                        const val = item.rawValue;
                        if (val && isValidProductBarcode(val, item.cornerPoints, item.boundingBox)) {
                          triggerScanSuccess(val);
                          return;
                        }
                      }
                    }
                  } catch (e) {
                    // ignore frame detect error
                  }
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

      // 2. Fallback to Html5Qrcode
      try {
        const html5QrCode = new Html5Qrcode(containerId, {
          formatsToSupport: [
            Html5QrcodeSupportedFormats.EAN_13,
            Html5QrcodeSupportedFormats.CODE_128,
            Html5QrcodeSupportedFormats.EAN_8,
            Html5QrcodeSupportedFormats.CODE_39,
            Html5QrcodeSupportedFormats.UPC_A
          ],
          verbose: false
        });

        html5QrCodeRef.current = html5QrCode;

        await html5QrCode.start(
          { facingMode: 'environment' },
          {
            fps: 20,
            qrbox: (viewfinderWidth, viewfinderHeight) => {
              // Horizontal slit box for horizontal ESL barcode
              return {
                width: Math.floor(viewfinderWidth * 0.85),
                height: Math.min(140, Math.floor(viewfinderHeight * 0.3))
              };
            },
            aspectRatio: window.innerHeight / window.innerWidth
          },
          (decodedText) => {
            if (isMounted && isValidProductBarcode(decodedText)) {
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

  // Cycle Zoom (1x -> 1.5x -> 2x)
  const cycleZoom = async () => {
    let nextZ = 1.0;
    if (zoomLevel <= 1.1) nextZ = Math.min(1.5, maxZoom);
    else if (zoomLevel <= 1.6) nextZ = Math.min(2.0, maxZoom);
    else nextZ = 1.0;

    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ zoom: nextZ }]
          });
          setZoomLevel(nextZ);
          return;
        }
      }
      if (html5QrCodeRef.current) {
        await (html5QrCodeRef.current as any).applyVideoConstraints({
          advanced: [{ zoom: nextZ }]
        });
        setZoomLevel(nextZ);
      }
    } catch (e) {
      console.warn('Zoom change failed:', e);
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
            ctx.filter = 'contrast(2.0) brightness(1.1) grayscale(1)';
            ctx.drawImage(bitmap, 0, 0);
          }

          const detector = new (window as any).BarcodeDetector();
          const results = await detector.detect(canvas);

          if (results && results.length > 0) {
            for (const r of results) {
              if (r.rawValue && isValidProductBarcode(r.rawValue, r.cornerPoints, r.boundingBox)) {
                triggerScanSuccess(r.rawValue);
                return;
              }
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
      if (isValidProductBarcode(decoded)) {
        triggerScanSuccess(decoded);
      } else {
        setCameraError('Wykryto kod etykiety zamiast kodu produktu. Nakieruj na poziomy kod pod ceną.');
      }
    } catch (err) {
      console.warn('Scan file error:', err);
      setCameraError('Nie udało się odczytać kodu kreskowego. Upewnij się, że poziomy kod pod ceną jest ostry.');
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

      {/* Video Viewport */}
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
              title="Latarka (eliminuje odblaski na ekranie ESL)"
            >
              <Flashlight className="w-5 h-5" />
            </button>
          )}

          {/* Zoom toggle button */}
          {hasZoom && (
            <button
              onClick={cycleZoom}
              className="w-11 h-11 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center backdrop-blur-xs text-xs font-black shadow-lg"
              title="Przełącz zoom (1.5x/2x zapobiega rozmyciu makro)"
            >
              {zoomLevel.toFixed(1)}×
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

      {/* Central ESL Horizontal Viewfinder Box */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center pointer-events-none p-4">
        {/* Horizontal slot perfectly sized for the horizontal product barcode */}
        <div
          className={`relative w-[88vw] max-w-[360px] h-[135px] sm:h-[145px] rounded-[26px] border-[3.5px] transition-all flex items-center justify-center ${
            scannedFeedback
              ? 'border-emerald-400 bg-emerald-500/20'
              : 'border-[#22c55e]'
          }`}
          style={{
            boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.44)'
          }}
        >
          {/* Laser aiming line across the horizontal barcode */}
          <div className="absolute inset-x-4 h-[2px] bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.95)] animate-pulse" />

          {/* Scan success indicator */}
          {scannedFeedback && (
            <div className="bg-black/85 text-emerald-400 px-3 py-1.5 rounded-full flex items-center gap-1.5 font-bold text-xs">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Odczytano: {scannedFeedback}</span>
            </div>
          )}
        </div>

        {/* Clear user guidance */}
        <p className="mt-4 text-xs font-bold text-white text-center drop-shadow-md bg-black/60 px-3.5 py-1.5 rounded-full backdrop-blur-xs max-w-xs">
          Nakieruj zieloną ramkę na <span className="text-[#22c55e]">poziomy kod produktu</span> pod ceną
        </p>
      </div>

      {/* Bottom Area with "Skan z zdjęcia" Blue Button */}
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
