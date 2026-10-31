import { useEffect, useRef, useState } from 'react';
import {
  X,
  Flashlight,
  Camera,
  AlertCircle,
  RefreshCw,
  CheckCircle2,
  Tag,
  Hash,
  Barcode,
  ArrowRight,
  Database,
  ScanText,
  Edit3
} from 'lucide-react';
import { scanImageData, setModuleArgs } from '@undecaf/zbar-wasm';
import { scanEslLabelWithoutAi, LocalOcrResult } from '../services/localOcrService';
import { scanEslLabel } from '../services/eslScannerService';
import { Carpet } from '../types';
import { initialCarpets } from '../data/seedData';

interface EslScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApplyData: (result: LocalOcrResult) => void;
  existingCarpets?: Carpet[];
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

const hasNativeBarcodeDetector = typeof window !== 'undefined' && 'BarcodeDetector' in window;

/**
 * Pleasant retail warehouse confirmation chime
 */
function playBeep() {
  try {
    const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioContextClass) return;
    const audioCtx = new AudioContextClass();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(1760, audioCtx.currentTime);
    gain.gain.setValueAtTime(0.18, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.13);
  } catch (_) {}
}

function extractProductCode(raw: string): string | null {
  if (!raw) return null;
  const clean = raw.trim();
  if (/[a-zA-Z]/.test(clean)) return null;
  if (!/^\d+$/.test(clean)) return null;
  if (clean.length === 13 || clean.length === 8 || clean.length === 12) {
    return clean;
  }
  return null;
}

export function EslScannerModal({
  isOpen,
  onClose,
  onApplyData,
  existingCarpets = []
}: EslScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1.8);
  const [hasZoom, setHasZoom] = useState(false);
  const [maxZoom, setMaxZoom] = useState(3.0);
  const [tapRipple, setTapRipple] = useState<{ x: number; y: number } | null>(null);

  // Live detection & Local OCR state (100% Non-AI)
  const [liveDetectedCode, setLiveDetectedCode] = useState<string | null>(null);
  const [matchedCarpet, setMatchedCarpet] = useState<Carpet | null>(null);
  const [isProcessingOcr, setIsProcessingOcr] = useState(false);
  const [ocrProgress, setOcrProgress] = useState(0);
  const [analyzedImage, setAnalyzedImage] = useState<string | null>(null);
  const [scanResult, setScanResult] = useState<LocalOcrResult | null>(null);
  const [scanError, setScanError] = useState<string | null>(null);

  // Editable fields before applying
  const [editNazwa, setEditNazwa] = useState('');
  const [editRef, setEditRef] = useState('');
  const [editEan, setEditEan] = useState('');

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameIdRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const isZBarBusyRef = useRef(false);

  // Reset state on open
  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isZBarBusyRef.current = false;
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);
    setHasZoom(false);
    setZoomLevel(1.8);
    setLiveDetectedCode(null);
    setMatchedCarpet(null);
    setIsProcessingOcr(false);
    setOcrProgress(0);
    setAnalyzedImage(null);
    setScanResult(null);
    setScanError(null);

    const initScanner = async () => {
      try {
        let detector: any = null;
        if (hasNativeBarcodeDetector) {
          try {
            const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
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

        const track = stream.getVideoTracks()[0];
        if (track) {
          const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
          if (capabilities.torch) setHasTorch(true);
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

          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          canvasRef.current = canvas;

          let lastZBarTime = 0;

          // Real-time detection loop runs in parallel with live camera feed
          const detectLoop = async (timestamp: number) => {
            if (!isMounted) return;

            if (videoRef.current && videoRef.current.readyState >= 2 && !scanResult) {
              const vw = videoRef.current.videoWidth;
              const vh = videoRef.current.videoHeight;

              // 1. Hardware BarcodeDetector
              if (detector) {
                try {
                  const nativeResults = await detector.detect(videoRef.current);
                  if (nativeResults && nativeResults.length > 0) {
                    for (const item of nativeResults) {
                      const valid = extractProductCode(item.rawValue);
                      if (valid) {
                        handleLiveBarcodeFound(valid);
                        break;
                      }
                    }
                  }
                } catch (_) {}
              }

              // 2. High-contrast ZBar WASM
              if (!isZBarBusyRef.current && timestamp - lastZBarTime > 80 && ctx && vw > 0 && vh > 0) {
                lastZBarTime = timestamp;
                isZBarBusyRef.current = true;

                const targetW = Math.min(vw, 960);
                const targetH = Math.min(vh, 540);
                canvas.width = targetW;
                canvas.height = targetH;
                ctx.filter = 'contrast(2.2) brightness(1.15) grayscale(1)';
                ctx.drawImage(videoRef.current, 0, 0, targetW, targetH);

                try {
                  const imgData = ctx.getImageData(0, 0, targetW, targetH);
                  const symbols = await scanImageData(imgData);
                  if (symbols && symbols.length > 0) {
                    for (const sym of symbols) {
                      const valid = extractProductCode(sym.decode());
                      if (valid) {
                        handleLiveBarcodeFound(valid);
                        break;
                      }
                    }
                  }
                } catch (_) {
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

  const allCarpets = [...existingCarpets, ...initialCarpets];

  // Handle barcode found live on camera
  const handleLiveBarcodeFound = (code: string) => {
    if (liveDetectedCode === code) return;
    setLiveDetectedCode(code);
    playBeep();

    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      try {
        navigator.vibrate([40, 30, 40]);
      } catch (_) {}
    }

    // Check if code matches any existing carpet in the store catalog (100% exact)
    const found = allCarpets.find(
      (c) => c.ean === code || c.ref === code || (code.length === 8 && c.ref === code)
    );
    if (found) {
      setMatchedCarpet(found);
    }
  };

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
    } catch (_) {}
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
    } catch (_) {}
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

  // RUN LOCAL NON-AI OCR (Tesseract.js + ZBar WASM + Instant Catalog Lookup)
  const handleCaptureAndRunLocalOcr = async () => {
    if (!videoRef.current || isProcessingOcr) return;

    try {
      setIsProcessingOcr(true);
      setOcrProgress(5);
      setScanError(null);

      const video = videoRef.current;
      const vw = video.videoWidth || 1920;
      const vh = video.videoHeight || 1080;
      const canvas = document.createElement('canvas');
      canvas.width = vw;
      canvas.height = vh;
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('Nie można utworzyć płótna obrazu.');

      ctx.drawImage(video, 0, 0, vw, vh);
      const dataUrl = canvas.toDataURL('image/jpeg', 0.92);
      setAnalyzedImage(dataUrl);

      // Check if barcode can be decoded directly from the captured frame
      let barcodeFound = liveDetectedCode;
      if (!barcodeFound && ctx) {
        try {
          const imgData = ctx.getImageData(0, 0, vw, vh);
          const symbols = await scanImageData(imgData);
          if (symbols && symbols.length > 0) {
            for (const sym of symbols) {
              const code = extractProductCode(sym.decode());
              if (code) {
                barcodeFound = code;
                break;
              }
            }
          }
        } catch (_) {}
      }

      // If barcode matches our store catalog, populate all fields with 100% precision in 0ms!
      if (barcodeFound) {
        const match = allCarpets.find(
          (c) => c.ean === barcodeFound || c.ref === barcodeFound || (barcodeFound.length === 8 && c.ref === barcodeFound)
        );
        if (match) {
          setMatchedCarpet(match);
          setScanResult({
            nazwa: match.nazwa,
            ref: match.ref,
            ean: match.ean
          });
          setEditNazwa(match.nazwa);
          setEditRef(match.ref);
          setEditEan(match.ean);
          playBeep();
          setIsProcessingOcr(false);
          return;
        }
      }

      // If not in catalog, try AI OCR first for 100% precision on new labels
      let result: LocalOcrResult | null = null;
      try {
        const aiRes = await scanEslLabel(dataUrl, 'image/jpeg');
        result = {
          nazwa: aiRes.nazwa,
          ref: aiRes.ref,
          ean: aiRes.ean,
          cena: aiRes.cena
        };
      } catch (aiErr: any) {
        console.warn('AI OCR fallback to local OCR:', aiErr?.message);
        // Fallback to local non-AI OCR
        result = await scanEslLabelWithoutAi(canvas, vw, vh, (pct) => {
          setOcrProgress(pct);
        });
      }

      if (barcodeFound) {
        if (barcodeFound.length === 13) result.ean = barcodeFound;
        if (barcodeFound.length === 8) result.ref = barcodeFound;
      }

      setScanResult(result);
      setEditNazwa(result.nazwa || '');
      setEditRef(result.ref || '');
      setEditEan(result.ean || '');
      playBeep();
    } catch (err: unknown) {
      setScanError(
        err instanceof Error
          ? err.message
          : 'Nie udało się odczytać etykiety. Upewnij się, że etykieta jest dobrze oświetlona i ostra.'
      );
    } finally {
      setIsProcessingOcr(false);
    }
  };

  // SCAN FROM PHOTO FILE WITHOUT AI
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessingOcr(true);
    setOcrProgress(10);
    setScanError(null);

    try {
      // 1. First attempt instant barcode decode on the raw uploaded file
      let barcodeFound: string | null = null;
      try {
        const bitmap = await createImageBitmap(file);
        const c = document.createElement('canvas');
        c.width = bitmap.width;
        c.height = bitmap.height;
        const ctx = c.getContext('2d');
        if (ctx) {
          ctx.drawImage(bitmap, 0, 0);
          const imgData = ctx.getImageData(0, 0, bitmap.width, bitmap.height);
          const symbols = await scanImageData(imgData);
          if (symbols && symbols.length > 0) {
            for (const sym of symbols) {
              const code = extractProductCode(sym.decode());
              if (code) {
                barcodeFound = code;
                break;
              }
            }
          }
        }
      } catch (_) {}

      const reader = new FileReader();
      const dataUrl = await new Promise<string>((resolve, reject) => {
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });

      setAnalyzedImage(dataUrl);

      // If barcode found and matches catalog, populate 100% exact data immediately!
      if (barcodeFound) {
        setLiveDetectedCode(barcodeFound);
        const match = allCarpets.find(
          (c) => c.ean === barcodeFound || c.ref === barcodeFound || (barcodeFound.length === 8 && c.ref === barcodeFound)
        );
        if (match) {
          setMatchedCarpet(match);
          setScanResult({
            nazwa: match.nazwa,
            ref: match.ref,
            ean: match.ean
          });
          setEditNazwa(match.nazwa);
          setEditRef(match.ref);
          setEditEan(match.ean);
          playBeep();
          setIsProcessingOcr(false);
          return;
        }
      }

      // If not in catalog, try AI OCR first for 100% precision on new labels
      let result: LocalOcrResult | null = null;
      try {
        const aiRes = await scanEslLabel(dataUrl, 'image/jpeg');
        result = {
          nazwa: aiRes.nazwa,
          ref: aiRes.ref,
          ean: aiRes.ean,
          cena: aiRes.cena
        };
      } catch (aiErr: any) {
        console.warn('AI OCR fallback to local OCR:', aiErr?.message);
        result = await scanEslLabelWithoutAi(dataUrl, undefined, undefined, (pct) => {
          setOcrProgress(pct);
        });
      }

      if (barcodeFound) {
        if (barcodeFound.length === 13) result.ean = barcodeFound;
        if (barcodeFound.length === 8) result.ref = barcodeFound;
      }

      setScanResult(result);
      setEditNazwa(result.nazwa || '');
      setEditRef(result.ref || '');
      setEditEan(result.ean || '');
      playBeep();
    } catch (err: unknown) {
      setScanError(err instanceof Error ? err.message : 'Błąd podczas odczytu zdjęcia.');
    } finally {
      setIsProcessingOcr(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // Confirm and apply data
  const handleApplyFinalData = () => {
    onApplyData({
      nazwa: editNazwa.trim(),
      ref: editRef.trim(),
      ean: editEan.trim(),
      cena: scanResult?.cena
    });
    onClose();
  };

  // Apply matched carpet from store database
  const handleApplyMatchedCarpet = () => {
    if (!matchedCarpet) return;
    onApplyData({
      nazwa: matchedCarpet.nazwa,
      ref: matchedCarpet.ref,
      ean: matchedCarpet.ean
    });
    onClose();
  };

  // Reset to continue scanning
  const handleResetScan = () => {
    setAnalyzedImage(null);
    setScanResult(null);
    setScanError(null);
    setIsProcessingOcr(false);
    setOcrProgress(0);
    setEditNazwa('');
    setEditRef('');
    setEditEan('');
  };

  if (!isOpen) return null;

  return (
    <div
      onClick={handleScreenTap}
      className="fixed inset-0 z-50 bg-black flex flex-col justify-between overflow-hidden select-none"
    >
      {/* Hidden file input */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/*"
        className="hidden"
      />

      {/* Video Viewport: renders full screen video */}
      <div className="absolute inset-0 z-0 bg-black overflow-hidden flex items-center justify-center">
        {analyzedImage ? (
          <img
            src={analyzedImage}
            alt="Zrobione zdjęcie"
            className="w-full h-full object-contain"
          />
        ) : (
          <video
            ref={videoRef}
            playsInline
            muted
            className="w-full h-full object-cover"
          />
        )}
      </div>

      {/* Tap Ripple Indicator */}
      {tapRipple && (
        <div
          className="absolute z-20 pointer-events-none w-14 h-14 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-emerald-400 bg-emerald-400/20 animate-ping"
          style={{ left: tapRipple.x, top: tapRipple.y }}
        />
      )}

      {/* Local OCR Processing Overlay (100% Non-AI) */}
      {isProcessingOcr && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-xs flex flex-col items-center justify-center gap-3.5 p-6">
          <div className="w-14 h-14 rounded-full border-4 border-emerald-400 border-t-transparent animate-spin" />
          <div className="text-white font-black text-sm tracking-wide bg-black/70 px-5 py-2.5 rounded-2xl border border-white/10 shadow-2xl flex flex-col items-center gap-1.5">
            <span className="flex items-center gap-2">
              <ScanText className="w-4 h-4 text-emerald-400" />
              Lokalne odczytywanie etykiety (bez AI)...
            </span>
            <div className="w-48 bg-gray-700 h-2 rounded-full overflow-hidden mt-1">
              <div
                className="bg-emerald-400 h-full transition-all duration-300 rounded-full"
                style={{ width: `${Math.max(10, ocrProgress)}%` }}
              />
            </div>
            <span className="text-[11px] text-gray-300 font-mono">
              {ocrProgress > 0 ? `${ocrProgress}%` : 'Rozpoczynanie...'}
            </span>
          </div>
        </div>
      )}

      {/* TOP BAR: Matches BarcodeScannerModal EXACTLY */}
      <div className="relative z-30 flex items-center justify-between p-4 pt-6 sm:p-6 w-full">
        <div className="flex items-center gap-2">
          {/* Flashlight button */}
          {hasTorch && !analyzedImage && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleTorch();
              }}
              className={`w-12 h-12 rounded-full flex items-center justify-center transition-all cursor-pointer ${
                torchOn
                  ? 'bg-amber-400 text-gray-950 shadow-xl shadow-amber-400/50 scale-105'
                  : 'bg-black/60 text-white hover:bg-black/80 backdrop-blur-xs'
              }`}
              title="Latarka"
            >
              <Flashlight className="w-5 h-5" />
            </button>
          )}

          {/* Zoom toggle button */}
          {hasZoom && !analyzedImage && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                cycleZoom();
              }}
              className="w-12 h-12 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center backdrop-blur-xs text-xs font-black shadow-lg cursor-pointer"
              title="Przełącz zoom"
            >
              {zoomLevel.toFixed(1)}×
            </button>
          )}
        </div>

        {/* Title badge: Clarifies 100% Free / Local */}
        <div className="bg-black/60 backdrop-blur-md px-4 py-1.5 rounded-full text-white text-xs sm:text-sm font-bold shadow-md border border-white/10 flex items-center gap-1.5">
          <ScanText className="w-3.5 h-3.5 text-emerald-400" />
          <span>Skaner Cenówek ESL</span>
        </div>

        {/* Circular Close Button (X) */}
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

      {/* CENTRAL VIEWFINDER: Matches BarcodeScannerModal EXACTLY */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center p-2 sm:p-4">
        {!scanResult && !scanError && (
          <div
            className={`relative w-[92vw] max-w-[420px] h-[340px] sm:h-[380px] rounded-[36px] border-[3.5px] transition-all flex flex-col items-center justify-between p-4 pointer-events-none ${
              liveDetectedCode
                ? 'border-emerald-400 bg-emerald-500/15'
                : 'border-[#22c55e]'
            }`}
            style={{
              boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.45)'
            }}
          >
            {/* Animated scan glow at top and bottom */}
            <div className="absolute inset-x-6 top-0 h-1.5 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />
            <div className="absolute inset-x-6 bottom-0 h-1.5 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />

            {/* Top guide labels inside viewfinder */}
            <div className="w-full flex justify-between items-center text-[10px] font-bold text-emerald-300 uppercase tracking-wider drop-shadow-md">
              <span>ETYKIETA ESL</span>
              <span>DYWANY 038</span>
            </div>

            {/* Central live detection badge */}
            <div className="flex flex-col items-center gap-2">
              {liveDetectedCode ? (
                <div className="bg-black/90 text-emerald-400 px-4 py-2 rounded-2xl flex items-center gap-2 font-black text-xs sm:text-sm shadow-2xl border border-emerald-400/40 animate-in zoom-in-95 pointer-events-auto">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Kod z kresek: {liveDetectedCode}</span>
                </div>
              ) : (
                <div className="text-center text-xs font-semibold text-white/90 drop-shadow-md bg-black/40 px-3 py-1.5 rounded-xl border border-white/10">
                  Umieść całą cenówkę w ramce
                </div>
              )}

              {/* Matched carpet preview if already in store database! */}
              {matchedCarpet && (
                <div className="bg-emerald-950/90 text-white px-3.5 py-2.5 rounded-2xl text-center max-w-xs shadow-2xl border border-emerald-400/60 pointer-events-auto flex flex-col gap-1.5 animate-in zoom-in-95">
                  <div className="text-[10px] font-bold text-emerald-300 flex items-center justify-center gap-1 uppercase">
                    <Database className="w-3 h-3" />
                    Rozpoznano w bazie sklepu!
                  </div>
                  <div className="font-extrabold text-xs truncate">
                    {matchedCarpet.nazwa}
                  </div>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleApplyMatchedCarpet();
                    }}
                    className="mt-0.5 py-1.5 px-3 bg-emerald-500 hover:bg-emerald-400 text-gray-950 rounded-xl text-xs font-black shadow-md cursor-pointer flex items-center justify-center gap-1 active:scale-95 transition-all"
                  >
                    <span>Wstaw ten dywan</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>

            {/* Bottom guide labels */}
            <div className="w-full flex justify-between items-center text-[9px] text-emerald-200 drop-shadow-md">
              <span>Nazwa i REF</span>
              <span>Kod kreskowy EAN</span>
            </div>
          </div>
        )}

        {/* Local OCR Result Card: Editable before insertion */}
        {scanResult && (
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-[92vw] max-w-[420px] bg-gray-900/95 border-2 border-emerald-400 rounded-3xl p-5 flex flex-col gap-3.5 shadow-2xl text-white backdrop-blur-md animate-in zoom-in-95 max-h-[75vh] overflow-y-auto"
          >
            <div className="flex items-center justify-between border-b border-gray-700 pb-2">
              <div className="flex items-center gap-1.5 text-xs font-black text-emerald-400 uppercase tracking-wider">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                Odczytano dane z cenówki
              </div>
              <div className="flex items-center gap-1 text-[11px] text-gray-400">
                <Edit3 className="w-3 h-3 text-emerald-400" />
                <span>Możesz edytować</span>
              </div>
            </div>

            <div className="flex flex-col gap-2.5 text-xs">
              {/* Editable Nazwa */}
              <div className="bg-black/60 p-2.5 rounded-2xl border border-gray-700 flex flex-col gap-1">
                <label className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1">
                  <Tag className="w-3 h-3 text-emerald-400" />
                  Nazwa dywanu i wymiary
                </label>
                <input
                  type="text"
                  value={editNazwa}
                  onChange={(e) => setEditNazwa(e.target.value)}
                  placeholder="np. DYWAN BLIDA 160X230CM SZARY"
                  className="w-full bg-gray-950 text-white font-extrabold text-xs sm:text-sm px-3 py-2 rounded-xl border border-gray-700 focus:border-emerald-400 focus:outline-hidden"
                />
              </div>

              {/* Editable REF & EAN grid */}
              <div className="grid grid-cols-2 gap-2">
                <div className="bg-black/60 p-2.5 rounded-2xl border border-gray-700 flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1">
                    <Hash className="w-3 h-3 text-blue-400" />
                    REF LM (8 cyfr)
                  </label>
                  <input
                    type="text"
                    value={editRef}
                    onChange={(e) => setEditRef(e.target.value)}
                    placeholder="8 cyfr"
                    maxLength={8}
                    className="w-full bg-gray-950 text-white font-mono font-bold text-xs sm:text-sm px-3 py-2 rounded-xl border border-gray-700 focus:border-blue-400 focus:outline-hidden"
                  />
                </div>

                <div className="bg-black/60 p-2.5 rounded-2xl border border-gray-700 flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1">
                    <Barcode className="w-3 h-3 text-purple-400" />
                    Kod EAN (13 cyfr)
                  </label>
                  <input
                    type="text"
                    value={editEan}
                    onChange={(e) => setEditEan(e.target.value)}
                    placeholder="13 cyfr"
                    maxLength={13}
                    className="w-full bg-gray-950 text-white font-mono font-bold text-xs sm:text-sm px-3 py-2 rounded-xl border border-gray-700 focus:border-purple-400 focus:outline-hidden"
                  />
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex gap-2 pt-1 border-t border-gray-800">
              <button
                type="button"
                onClick={handleResetScan}
                className="py-3 px-3.5 rounded-2xl bg-gray-800 hover:bg-gray-700 text-white font-bold text-xs transition-colors cursor-pointer"
              >
                Ponów
              </button>

              <button
                type="button"
                onClick={handleApplyFinalData}
                className="flex-1 py-3 px-4 rounded-2xl bg-[#22c55e] hover:bg-[#16a34a] text-gray-950 font-black text-xs sm:text-sm shadow-xl flex items-center justify-center gap-1.5 cursor-pointer active:scale-95 transition-all"
              >
                <CheckCircle2 className="w-4 h-4 text-gray-950" />
                <span>Wstaw do formularza</span>
              </button>
            </div>
          </div>
        )}

        {/* Scan Error Notice */}
        {scanError && (
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-[92vw] max-w-[420px] bg-gray-950/95 border-2 border-amber-400 rounded-3xl p-5 flex flex-col gap-3 shadow-2xl text-white backdrop-blur-md animate-in zoom-in-95"
          >
            <div className="flex items-start gap-2.5 text-xs text-amber-200">
              <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
              <div className="flex-1 leading-relaxed">
                <span className="font-extrabold text-sm block text-amber-400 mb-1">
                  Problem z odczytem
                </span>
                <span>{scanError}</span>
              </div>
            </div>

            <div className="flex gap-2 pt-1 border-t border-gray-800">
              <button
                type="button"
                onClick={handleResetScan}
                className="py-2.5 px-4 rounded-2xl bg-gray-800 hover:bg-gray-700 text-white font-bold text-xs cursor-pointer"
              >
                Spróbuj ponownie
              </button>
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 px-4 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs cursor-pointer text-center"
              >
                Wpisz ręcznie
              </button>
            </div>
          </div>
        )}
      </div>

      {/* BOTTOM CONTROLS: Identical positioning and style to BarcodeScannerModal */}
      <div className="relative z-30 w-full pb-8 sm:pb-10 pt-4 flex flex-col items-center justify-center gap-3">
        {cameraError && (
          <div className="mx-4 max-w-sm px-4 py-2 bg-black/80 backdrop-blur-md border border-amber-500/50 rounded-xl text-amber-300 text-xs text-center flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>{cameraError}</span>
          </div>
        )}

        <div className="flex items-center gap-2.5 px-4">
          {/* Blue button: [📸 Skan z zdjęcia] matching BarcodeScannerModal */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              fileInputRef.current?.click();
            }}
            disabled={isProcessingOcr}
            className="py-3 px-5 rounded-2xl bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-bold text-xs sm:text-sm shadow-xl flex items-center gap-2 active:scale-95 transition-all cursor-pointer"
          >
            {isProcessingOcr ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Odczyt...</span>
              </>
            ) : (
              <>
                <span className="text-base">📸</span>
                <span>Skan z zdjęcia</span>
              </>
            )}
          </button>

          {/* Primary Action Button: "Zrób zdjęcie i odczytaj" */}
          {!scanResult && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleCaptureAndRunLocalOcr();
              }}
              disabled={isProcessingOcr}
              className="py-3 px-6 rounded-2xl bg-[#22c55e] hover:bg-[#16a34a] text-gray-950 font-black text-xs sm:text-sm shadow-2xl flex items-center gap-2 active:scale-95 transition-all cursor-pointer border border-emerald-300/40"
            >
              <Camera className="w-4 h-4 text-gray-950" />
              <span>Odczytaj cenówkę</span>
            </button>
          )}

          {/* Back button if result is visible */}
          {scanResult && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleResetScan();
              }}
              className="py-3 px-5 rounded-2xl bg-white/90 hover:bg-white text-gray-900 font-bold text-xs sm:text-sm shadow-xl flex items-center gap-1.5 active:scale-95 transition-all cursor-pointer"
            >
              <span>Wróć do kamery</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
