import { useEffect, useRef, useState } from 'react';
import { X, Flashlight, AlertCircle, RefreshCw, CheckCircle2 } from 'lucide-react';
import { scanImageData, setModuleArgs } from '@undecaf/zbar-wasm';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (decodedText: string) => void;
}

// Configure ZBar WebAssembly path
if (typeof window !== 'undefined') {
  try {
    const baseUrl = import.meta.env.BASE_URL || '/';
    setModuleArgs({
      locateFile: (file: string) => `${baseUrl}${file}`
    });
  } catch (_) {}
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
 * Validates and extracts true product barcode from ESL label:
 * Accepts 13-digit EAN, 8-digit REF / EAN-8, 12-digit UPC (Code 128 or EAN-13 format).
 * Automatically discards 16-24 digit electronic tag serials (e.g. 952401350807681623)
 * and alphanumeric IDs.
 */
function extractProductCode(raw: string): string | null {
  if (!raw) return null;
  const clean = raw.trim();

  // If it contains letters, it's an ESL hardware tag ID
  if (/[a-zA-Z]/.test(clean)) return null;

  // Must be pure digits
  if (!/^\d+$/.test(clean)) return null;

  // Standard product barcodes: 13 digits (EAN-13), 8 digits (REF), 12 digits (UPC)
  if (clean.length === 13 || clean.length === 8 || clean.length === 12) {
    return clean;
  }

  return null;
}

export function BarcodeScannerModal({ isOpen, onClose, onScanSuccess }: BarcodeScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1.8);
  const [hasZoom, setHasZoom] = useState(false);
  const [maxZoom, setMaxZoom] = useState(3.0);
  const [isScanningFile, setIsScanningFile] = useState(false);
  const [scannedFeedback, setScannedFeedback] = useState<string | null>(null);
  const [tapRipple, setTapRipple] = useState<{ x: number; y: number } | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameIdRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const isScannedRef = useRef(false);
  const isZBarBusyRef = useRef(false);

  const triggerScanSuccess = (code: string) => {
    if (isScannedRef.current) return;
    isScannedRef.current = true;
    setScannedFeedback(code);

    playBeep();

    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate([60, 40, 60]);
      } catch (_) {}
    }

    setTimeout(() => {
      onScanSuccess(code.trim());
      onClose();
    }, 180);
  };

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isScannedRef.current = false;
    isZBarBusyRef.current = false;
    setScannedFeedback(null);
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);
    setHasZoom(false);
    setZoomLevel(1.8);

    const initScanner = async () => {
      try {
        // Initialize detector if available
        let detector: any = null;
        if (hasNativeBarcodeDetector) {
          try {
            const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
            // Critical: ESL labels format the barcode as Code 128 as well as EAN-13
            const targetFormats = ['code_128', 'ean_13', 'ean_8', 'upc_a', 'code_39', 'itf'].filter((f) =>
              supportedFormats.includes(f)
            );
            detector = new (window as any).BarcodeDetector({
              formats: targetFormats.length > 0 ? targetFormats : supportedFormats
            });
          } catch (e) {
            console.warn('Native BarcodeDetector init error:', e);
          }
        }

        // Request high resolution camera stream
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

        // Check capabilities
        const track = stream.getVideoTracks()[0];
        if (track) {
          const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
          if (capabilities.torch) {
            setHasTorch(true);
          }
          if (capabilities.zoom) {
            setHasZoom(true);
            const maxZ = Math.min(capabilities.zoom.max || 3.0, 3.0);
            const defaultZ = Math.min(1.8, maxZ);
            setMaxZoom(maxZ);
            setZoomLevel(defaultZ);

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

          // Offscreen canvas for ZBar WASM and low-light enhancement
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          canvasRef.current = canvas;

          let lastZBarTime = 0;

          const detectLoop = async (timestamp: number) => {
            if (!isMounted || isScannedRef.current) return;

            if (videoRef.current && videoRef.current.readyState >= 2) {
              const vw = videoRef.current.videoWidth;
              const vh = videoRef.current.videoHeight;

              // 1. ENGINE 1: Native Android BarcodeDetector (instant hardware check on raw video)
              if (detector) {
                try {
                  const nativeResults = await detector.detect(videoRef.current);
                  if (nativeResults && nativeResults.length > 0) {
                    for (const item of nativeResults) {
                      const validProduct = extractProductCode(item.rawValue);
                      if (validProduct) {
                        triggerScanSuccess(validProduct);
                        return;
                      }
                    }
                  }
                } catch (_) {}
              }

              // 2. ENGINE 2: Industrial ZBar WebAssembly (C library, unbeatable in low light, e-ink, and multi-angle)
              if (!isZBarBusyRef.current && timestamp - lastZBarTime > 60 && ctx && vw > 0 && vh > 0) {
                lastZBarTime = timestamp;
                isZBarBusyRef.current = true;

                // Use 960x540 for optimum sub-pixel performance in 40ms
                const targetW = Math.min(vw, 960);
                const targetH = Math.min(vh, 540);
                canvas.width = targetW;
                canvas.height = targetH;

                // Strong contrast curve for e-ink labels in weak light
                ctx.filter = 'contrast(2.2) brightness(1.15) grayscale(1)';
                ctx.drawImage(videoRef.current, 0, 0, targetW, targetH);

                try {
                  const imgData = ctx.getImageData(0, 0, targetW, targetH);
                  const symbols = await scanImageData(imgData);

                  if (symbols && symbols.length > 0) {
                    for (const sym of symbols) {
                      const decoded = sym.decode();
                      const validProduct = extractProductCode(decoded);
                      if (validProduct) {
                        triggerScanSuccess(validProduct);
                        return;
                      }
                    }
                  }
                } catch (zErr) {
                  // silent frame parse error
                } finally {
                  isZBarBusyRef.current = false;
                }
              }
            }

            animFrameIdRef.current = requestAnimationFrame(detectLoop);
          };

          animFrameIdRef.current = requestAnimationFrame(detectLoop);
        }
      } catch (err: unknown) {
        if (!isMounted) return;
        const errMsg = err instanceof Error ? err.message : String(err);
        console.warn('Camera startup error:', errMsg);
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
        }
      }
    } catch (e) {
      console.warn('Torch failed:', e);
    }
  };

  // Cycle Zoom (1.0x -> 1.8x -> 2.5x)
  const cycleZoom = async () => {
    let nextZ = 1.0;
    if (zoomLevel <= 1.2) nextZ = Math.min(1.8, maxZoom);
    else if (zoomLevel <= 2.0) nextZ = Math.min(2.5, maxZoom);
    else nextZ = 1.0;

    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ zoom: nextZ }]
          });
          setZoomLevel(nextZ);
        }
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

  // Scan from photo file using ZBar WASM
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsScanningFile(true);
    setCameraError(null);

    try {
      const bitmap = await createImageBitmap(file);
      const canvas = document.createElement('canvas');
      canvas.width = bitmap.width;
      canvas.height = bitmap.height;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(bitmap, 0, 0);
        const imgData = ctx.getImageData(0, 0, bitmap.width, bitmap.height);
        const symbols = await scanImageData(imgData);

        if (symbols && symbols.length > 0) {
          for (const s of symbols) {
            const valid = extractProductCode(s.decode());
            if (valid) {
              triggerScanSuccess(valid);
              return;
            }
          }
        }
      }
      setCameraError('Nie udało się odczytać kodu z tego zdjęcia. Upewnij się, że etykieta jest widoczna.');
    } catch (err) {
      console.warn('File scan error:', err);
      setCameraError('Błąd podczas odczytu zdjęcia.');
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
        <video
          ref={videoRef}
          playsInline
          muted
          className="w-full h-full object-cover"
        />
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
              title="Latarka"
            >
              <Flashlight className="w-5 h-5" />
            </button>
          )}

          {/* Zoom toggle button (1x / 1.8x / 2.5x) */}
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
        {/* Generous rounded frame */}
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
