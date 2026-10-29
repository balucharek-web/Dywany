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
 * Pleasant retail warehouse confirmation chime (Web Audio API)
 */
function playBeep() {
  try {
    const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioContextClass) return;
    const audioCtx = new AudioContextClass();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(1760, audioCtx.currentTime); // 1760Hz high retail chime
    gain.gain.setValueAtTime(0.18, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.13);
  } catch (_) {}
}

/**
 * EAN-13 Checksum verification
 */
function isValidEAN13Checksum(ean: string): boolean {
  if (!/^\d{13}$/.test(ean)) return false;
  let sum = 0;
  for (let i = 0; i < 12; i++) {
    sum += parseInt(ean[i], 10) * (i % 2 === 0 ? 1 : 3);
  }
  const check = (10 - (sum % 10)) % 10;
  return check === parseInt(ean[12], 10);
}

/**
 * Intelligent barcode classifier for retail ESL (Electronic Shelf Labels):
 * Immediately selects genuine product codes (EAN-13, REF, EAN-8) regardless of where
 * they appear in the frame (center, corner, top, bottom, or at any angle),
 * and ignores vertical electronic shelf tag hardware IDs.
 */
function pickBestProductBarcode(barcodes: any[]): string | null {
  if (!barcodes || barcodes.length === 0) return null;

  const candidates = barcodes.map((b) => {
    const raw = (b.rawValue || (typeof b === 'string' ? b : '')).trim();

    // 1. Is it a valid 13-digit EAN (product barcode)?
    const isEAN13 = /^\d{13}$/.test(raw);
    const hasValidChecksum = isEAN13 && isValidEAN13Checksum(raw);
    const isLMStandard = isEAN13 && (raw.startsWith('590') || raw.startsWith('327') || raw.startsWith('59') || raw.startsWith('32'));

    // 2. Is it an 8-digit REF number or EAN-8?
    const is8DigitRef = /^\d{8}$/.test(raw);

    // 3. Is it any numeric product code?
    const isNumeric = /^\d{7,14}$/.test(raw);

    // 4. Is it likely an ESL hardware serial/tag ID?
    const hasLetters = /[a-zA-Z]/.test(raw);

    // Scoring system prioritizing genuine product barcodes:
    let score = 0;
    if (hasValidChecksum) score += 300;
    else if (isLMStandard) score += 250;
    else if (isEAN13) score += 200;
    else if (is8DigitRef) score += 180;
    else if (isNumeric) score += 100;

    if (hasLetters) score -= 200;

    return { raw, score, isNumeric };
  });

  // Sort by highest score first
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
  const [zoomLevel, setZoomLevel] = useState(2.0);
  const [hasZoom, setHasZoom] = useState(false);
  const [maxZoom, setMaxZoom] = useState(3.0);
  const [isScanningFile, setIsScanningFile] = useState(false);
  const [scannedFeedback, setScannedFeedback] = useState<string | null>(null);
  const [tapRipple, setTapRipple] = useState<{ x: number; y: number } | null>(null);

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

    // Play warehouse confirmation chime
    playBeep();

    // Haptic vibration feedback
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate([60, 40, 60]);
      } catch (_) {}
    }

    // Brief visual confirmation before closing
    setTimeout(() => {
      onScanSuccess(code.trim());
      onClose();
    }, 180);
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
    setZoomLevel(2.0);

    const initScanner = async () => {
      // 1. PRIMARY ENGINE: Native Android Google BarcodeDetector (Full frame 360°, Any angle, Anywhere in frame, 60 FPS)
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

          // Check track capabilities (zoom, torch)
          const track = stream.getVideoTracks()[0];
          if (track) {
            const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
            if (capabilities.torch) {
              setHasTorch(true);
            }
            if (capabilities.zoom) {
              setHasZoom(true);
              const maxZ = Math.min(capabilities.zoom.max || 3.0, 3.0);
              const defaultZ = Math.min(2.0, maxZ);
              setMaxZoom(maxZ);
              setZoomLevel(defaultZ);

              // Automatically apply 2.0x zoom so ESL label fills the frame and small e-ink barcode is 3x larger on sensor
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

            // Prepare 1:1 full-resolution canvas for contrast boost (zero downscaling)
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d', { willReadFrequently: true });
            canvasRef.current = canvas;

            let frameCount = 0;

            const detectLoop = async () => {
              if (!isMounted || isScannedRef.current) return;

              if (videoRef.current && videoRef.current.readyState >= 2) {
                frameCount++;
                try {
                  // PASS 1: Native full-screen hardware detection on raw video
                  const rawResults = await detector.detect(videoRef.current);
                  if (rawResults && rawResults.length > 0) {
                    const bestCode = pickBestProductBarcode(rawResults);
                    if (bestCode) {
                      triggerScanSuccess(bestCode);
                      return;
                    }
                  }

                  // PASS 2 (every 2nd frame): 1:1 Full-Resolution Contrast Boost for e-paper labels
                  // NOTE: Zero downscaling ensures thin 1-pixel barcode bars are never blurred
                  if (frameCount % 2 === 0 && ctx) {
                    const vw = videoRef.current.videoWidth;
                    const vh = videoRef.current.videoHeight;
                    if (vw > 0 && vh > 0) {
                      canvas.width = vw;
                      canvas.height = vh;

                      // Strong contrast boost: turns grey e-paper background to pure white and barcode lines to pure black
                      ctx.filter = 'contrast(2.2) brightness(1.15) grayscale(1)';
                      ctx.drawImage(videoRef.current, 0, 0);

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

      // 2. FALLBACK ENGINE: Html5Qrcode with FULL-SCREEN scanning (no restrictive center qrbox)
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
          if (trackCaps.zoom) {
            setHasZoom(true);
            setMaxZoom(Math.min(trackCaps.zoom.max || 3.0, 3.0));
          }
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

  // Cycle Zoom (1.0x -> 2.0x -> 2.5x)
  const cycleZoom = async () => {
    let nextZ = 1.0;
    if (zoomLevel <= 1.2) nextZ = Math.min(2.0, maxZoom);
    else if (zoomLevel <= 2.1) nextZ = Math.min(2.5, maxZoom);
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

  // Tap-to-focus on screen
  const handleScreenTap = async (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    setTapRipple({ x, y });
    setTimeout(() => setTapRipple(null), 600);

    // Re-trigger camera focus if supported
    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ focusMode: 'continuous' }]
          });
        }
      }
    } catch (_) {}
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
        setCameraError('Wykryto kod etykiety zamiast kodu produktu. Upewnij się, że kod produktu jest widoczny.');
      }
    } catch (err) {
      console.warn('Scan file error:', err);
      setCameraError('Nie udało się odczytać kodu kreskowego z tego zdjęcia.');
    } finally {
      setIsScanningFile(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div
      onClick={handleScreenTap}
      className="fixed inset-0 z-50 bg-black flex flex-col justify-between overflow-hidden select-none"
    >
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

      {/* Tap Ripple Indicator */}
      {tapRipple && (
        <div
          className="absolute z-20 pointer-events-none w-14 h-14 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-emerald-400 bg-emerald-400/20 animate-ping"
          style={{ left: tapRipple.x, top: tapRipple.y }}
        />
      )}

      {/* Top Bar with circular Close (X) and Torch / Zoom buttons */}
      <div className="relative z-30 flex items-center justify-between p-4 pt-6 sm:p-6 w-full">
        <div className="flex items-center gap-2">
          {/* Flashlight button */}
          {hasTorch && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleTorch();
              }}
              className={`w-12 h-12 rounded-full flex items-center justify-center transition-all ${
                torchOn
                  ? 'bg-amber-400 text-gray-950 shadow-xl shadow-amber-400/50 scale-105'
                  : 'bg-black/60 text-white hover:bg-black/80 backdrop-blur-xs'
              }`}
              title="Latarka (do słabego oświetlenia)"
            >
              <Flashlight className="w-5 h-5" />
            </button>
          )}

          {/* Zoom toggle button (1x / 2x / 2.5x) */}
          {hasZoom && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                cycleZoom();
              }}
              className="w-12 h-12 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center backdrop-blur-xs text-xs font-black shadow-lg"
              title="Przełącz zoom"
            >
              {zoomLevel.toFixed(1)}×
            </button>
          )}
        </div>

        {/* Circular Close Button (X) top right */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onClose();
          }}
          className="w-12 h-12 rounded-full bg-black/70 hover:bg-black text-white flex items-center justify-center shadow-lg backdrop-blur-xs transition-transform active:scale-95 cursor-pointer"
          title="Zamknij skaner"
        >
          <X className="w-6 h-6 stroke-[2.5]" />
        </button>
      </div>

      {/* Central Viewfinder Box matching Screenshot */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center pointer-events-none p-2 sm:p-4">
        {/* Generous rounded frame that covers the entire ESL label comfortably */}
        <div
          className={`relative w-[92vw] max-w-[420px] h-[340px] sm:h-[380px] rounded-[36px] border-[3.5px] transition-all flex items-center justify-center ${
            scannedFeedback
              ? 'border-emerald-400 bg-emerald-500/25 scale-[1.02]'
              : 'border-[#22c55e]'
          }`}
          style={{
            boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.40)'
          }}
        >
          {/* Subtle animated scan glow at top and bottom */}
          <div className="absolute inset-x-6 top-0 h-1.5 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />
          <div className="absolute inset-x-6 bottom-0 h-1.5 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />

          {/* Scan success indicator */}
          {scannedFeedback && (
            <div className="bg-black/90 text-emerald-400 px-5 py-2.5 rounded-full flex items-center gap-2 font-black text-sm sm:text-base shadow-2xl border border-emerald-400/40">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <span>Odczytano: {scannedFeedback}</span>
            </div>
          )}
        </div>
      </div>

      {/* Bottom Area with "Skan z zdjęcia" Blue Button */}
      <div className="relative z-30 w-full pb-8 sm:pb-10 pt-4 flex flex-col items-center justify-center gap-3">
        {cameraError && (
          <div className="mx-4 max-w-sm px-4 py-2 bg-black/80 backdrop-blur-md border border-amber-500/50 rounded-xl text-amber-300 text-xs text-center flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>{cameraError}</span>
          </div>
        )}

        {/* Blue button: [📸 Skan z zdjęcia] */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            fileInputRef.current?.click();
          }}
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
