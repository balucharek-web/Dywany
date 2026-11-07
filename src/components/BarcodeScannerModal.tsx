import { useEffect, useRef, useState } from 'react';
import { X, Flashlight, AlertCircle, RefreshCw, CheckCircle2, ScanLine, Camera, Crosshair } from 'lucide-react';
import { scanImageData, setModuleArgs } from '@undecaf/zbar-wasm';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess: (decodedText: string) => void;
  title?: string;
  continuousMode?: boolean;
  toastMessage?: string | null;
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
    osc.frequency.setValueAtTime(1760, audioCtx.currentTime); // 1760Hz crisp warehouse chime
    gain.gain.setValueAtTime(0.20, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.13);
  } catch (_) {}
}

/**
 * Validates and extracts true product barcode from ESL shelf labels and carpets:
 * Accepts 13-digit EAN, 8-digit REF LM, 12-digit UPC (Code 128 or EAN-13 format).
 * Automatically discards 16-24 digit electronic tag hardware serials (e.g. 147450750897439390)
 * and alphanumeric IDs.
 */
function extractProductCode(raw: string): string | null {
  if (!raw) return null;
  const clean = raw.trim().replace(/\s+/g, '');

  // 1. Pure digits check
  if (/^\d+$/.test(clean)) {
    // Standard product barcodes: 7 to 14 digits (REF 8-digit, EAN-13, GTIN-14)
    // Rejects 16-24 digit ESL tag hardware serial numbers
    if (clean.length >= 7 && clean.length <= 14) {
      return clean;
    }
    return null;
  }

  // 2. If it's a URL or text containing an 8-digit Ref LM or 13-digit EAN
  const match = raw.match(/\b(59\d{11}|32\d{11}|40\d{11}|\d{8})\b/);
  if (match) {
    return match[1];
  }

  return null;
}

/**
 * Fast contrast normalization for ESL e-paper displays (takes ~1-2ms).
 * Expands dithered gray-on-gray bars into high-contrast black-and-white.
 */
function enhanceESLContrast(data: Uint8ClampedArray) {
  const len = data.length;
  let min = 255;
  let max = 0;
  // Sample every 32nd byte (every 8th pixel) for blazing speed
  for (let i = 0; i < len; i += 32) {
    const lum = (data[i] * 77 + data[i + 1] * 150 + data[i + 2] * 29) >> 8;
    if (lum < min) min = lum;
    if (lum > max) max = lum;
  }
  if (max > min && max - min < 185) {
    const range = max - min;
    const invRange = 255 / range;
    for (let i = 0; i < len; i += 4) {
      const lum = (data[i] * 77 + data[i + 1] * 150 + data[i + 2] * 29) >> 8;
      const v = Math.min(255, Math.max(0, ((lum - min) * invRange) | 0));
      data[i] = v;
      data[i + 1] = v;
      data[i + 2] = v;
    }
  }
}

export function BarcodeScannerModal({
  isOpen,
  onClose,
  onScanSuccess,
  title,
  continuousMode = false,
  toastMessage = null
}: BarcodeScannerModalProps) {
  // Scanner Mode: 'laser' (narrow horizontal beam for ESL labels at 1.5m) | 'standard' (full window for carpets/sheets)
  const [scanMode, setScanMode] = useState<'laser' | 'standard'>('laser');
  const scanModeRef = useRef<'laser' | 'standard'>('laser');

  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [isHardwareZoom, setIsHardwareZoom] = useState(false);
  const isHardwareZoomRef = useRef(false);
  const [zoomLevel, setZoomLevel] = useState(1.0);
  const zoomLevelRef = useRef(1.0);
  const [maxZoom, setMaxZoom] = useState(3.0);
  const [isScanningFile, setIsScanningFile] = useState(false);
  const [scannedFeedback, setScannedFeedback] = useState<string | null>(null);
  const [tapRipple, setTapRipple] = useState<{ x: number; y: number } | null>(null);

  // Multi-camera selection (rear cameras only)
  const [rearCameras, setRearCameras] = useState<MediaDeviceInfo[]>([]);
  const [currentCameraIndex, setCurrentCameraIndex] = useState(0);
  const selectedCameraIdRef = useRef<string | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameIdRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const isScannedRef = useRef(false);
  const isZBarBusyRef = useRef(false);
  const focusIntervalRef = useRef<any>(null);

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

    onScanSuccess(code.trim());

    if (!continuousMode) {
      setTimeout(() => {
        onClose();
      }, 180);
    } else {
      setTimeout(() => {
        isScannedRef.current = false;
        setScannedFeedback(null);
      }, 1000);
    }
  };

  // Switch between Laser Mode (ESL 1.5m) and Standard Mode
  const toggleScanMode = () => {
    const next = scanMode === 'laser' ? 'standard' : 'laser';
    setScanMode(next);
    scanModeRef.current = next;
  };

  useEffect(() => {
    scanModeRef.current = scanMode;
  }, [scanMode]);

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isScannedRef.current = false;
    isZBarBusyRef.current = false;
    setScannedFeedback(null);
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);
    setIsHardwareZoom(false);
    isHardwareZoomRef.current = false;
    setZoomLevel(1.0);
    zoomLevelRef.current = 1.0;

    const initScanner = async () => {
      try {
        // Initialize Native BarcodeDetector if available in Chrome
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

        // Request clean high-resolution stream from REAR camera (ALWAYS facingMode: environment)
        let stream: MediaStream;
        const selectedDeviceId = selectedCameraIdRef.current;

        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: {
              ...(selectedDeviceId
                ? { deviceId: { exact: selectedDeviceId } }
                : { facingMode: { ideal: 'environment' } }),
              width: { ideal: 3840, min: 1280 },
              height: { ideal: 2160, min: 720 }
            },
            audio: false
          });
        } catch (_) {
          stream = await navigator.mediaDevices.getUserMedia({
            video: {
              facingMode: { ideal: 'environment' }
            },
            audio: false
          });
        }

        if (!isMounted) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        streamRef.current = stream;

        // Once permission is granted, enumerate and populate ONLY rear cameras (excluding front/selfie)
        try {
          const devices = await navigator.mediaDevices.enumerateDevices();
          const videoInputs = devices.filter((d) => d.kind === 'videoinput');
          const backCameras = videoInputs.filter((d) => {
            const lbl = (d.label || '').toLowerCase();
            const isFront = lbl.includes('front') || lbl.includes('user') || lbl.includes('selfie') || lbl.includes('przód');
            return !isFront;
          });
          if (backCameras.length > 1) {
            setRearCameras(backCameras);
          }
        } catch (_) {}

        // Check hardware capabilities (torch, hardware zoom, autofocus)
        const track = stream.getVideoTracks()[0];
        if (track) {
          const capabilities = (track.getCapabilities ? track.getCapabilities() : {}) as any;
          if (capabilities.torch) {
            setHasTorch(true);
          }

          // Apply continuous autofocus and auto-exposure
          const advancedConstraints: any = {};
          if (capabilities.focusMode?.includes('continuous')) {
            advancedConstraints.focusMode = 'continuous';
          }
          if (capabilities.exposureMode?.includes('continuous')) {
            advancedConstraints.exposureMode = 'continuous';
          }
          if (Object.keys(advancedConstraints).length > 0) {
            try {
              await (track as any).applyConstraints({ advanced: [advancedConstraints] });
            } catch (_) {}
          }

          if (capabilities.zoom) {
            setIsHardwareZoom(true);
            isHardwareZoomRef.current = true;
            const maxZ = Math.min(capabilities.zoom.max || 4.0, 4.0);
            setMaxZoom(maxZ);
          } else {
            setIsHardwareZoom(false);
            isHardwareZoomRef.current = false;
            setMaxZoom(3.0);
          }
        }

        // Periodic continuous autofocus pulse (every 2.5s) to prevent camera driver focus freeze
        focusIntervalRef.current = setInterval(async () => {
          if (!streamRef.current || isScannedRef.current) return;
          try {
            const tr = streamRef.current.getVideoTracks()[0];
            if (tr && (tr as any).applyConstraints) {
              await (tr as any).applyConstraints({
                advanced: [{ focusMode: 'continuous', pointsOfInterest: [{ x: 0.5, y: 0.5 }] }]
              });
            }
          } catch (_) {}
        }, 2500);

        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();

          // Offscreen high-density canvas for professional ROI extraction
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          canvasRef.current = canvas;

          let lastZBarTime = 0;
          let frameIndex = 0;
          let cycleCount = 0;

          const detectLoop = async (timestamp: number) => {
            if (!isMounted || isScannedRef.current) return;

            const video = videoRef.current;
            if (video && video.readyState >= 2 && ctx) {
              const vw = video.videoWidth;
              const vh = video.videoHeight;

              if (vw > 0 && vh > 0) {
                frameIndex++;

                if (!isZBarBusyRef.current && timestamp - lastZBarTime > 35) {
                  lastZBarTime = timestamp;
                  isZBarBusyRef.current = true;
                  cycleCount++;

                  const currentZoom = zoomLevelRef.current || 1.0;
                  const currentMode = scanModeRef.current;

                  let targetW: number;
                  let targetH: number;

                  if (currentMode === 'laser') {
                    // PROFESSIONAL LASER MODE:
                    // Extracts a narrow horizontal beam (like Zebra SE4750 laser scanner)
                    // High-resolution crop right through the center where ESL shelf barcodes sit!
                    const z = Math.max(1.0, currentZoom);
                    const cropFactorW = 0.88 / z;
                    const cropFactorH = 0.18 / z;

                    const cropW = Math.max(180, Math.round(vw * cropFactorW));
                    const cropH = Math.max(70, Math.round(vh * cropFactorH));
                    const cropX = Math.round((vw - cropW) / 2);
                    const cropY = Math.round((vh - cropH) / 2);

                    targetW = Math.min(1280, Math.max(720, Math.round(cropW * (z > 1.2 ? 1.4 : 1.0))));
                    targetH = Math.round((cropH * targetW) / cropW);

                    if (canvas.width !== targetW || canvas.height !== targetH) {
                      canvas.width = targetW;
                      canvas.height = targetH;
                    }

                    ctx.drawImage(video, cropX, cropY, cropW, cropH, 0, 0, targetW, targetH);
                  } else {
                    // STANDARD MODE (Alternating Telephoto & Wide Window)
                    const isTelephotoPass = currentZoom > 1.2 || cycleCount % 2 === 1;

                    if (isTelephotoPass) {
                      const z = Math.max(1.0, currentZoom);
                      const cropW = Math.max(200, Math.round(vw * (0.65 / z)));
                      const cropH = Math.max(140, Math.round(vh * (0.40 / z)));
                      const cropX = Math.round((vw - cropW) / 2);
                      const cropY = Math.round((vh - cropH) / 2);

                      targetW = Math.min(1280, Math.max(640, cropW));
                      targetH = Math.round((cropH * targetW) / cropW);

                      if (canvas.width !== targetW || canvas.height !== targetH) {
                        canvas.width = targetW;
                        canvas.height = targetH;
                      }

                      ctx.drawImage(video, cropX, cropY, cropW, cropH, 0, 0, targetW, targetH);
                    } else {
                      const maxDim = 1280;
                      if (vw >= vh) {
                        targetW = Math.min(maxDim, vw);
                        targetH = Math.round((vh * maxDim) / vw);
                      } else {
                        targetH = Math.min(maxDim, vh);
                        targetW = Math.round((vw * maxDim) / vh);
                      }

                      if (canvas.width !== targetW || canvas.height !== targetH) {
                        canvas.width = targetW;
                        canvas.height = targetH;
                      }

                      ctx.drawImage(video, 0, 0, targetW, targetH);
                    }
                  }

                  try {
                    // 1. ENGINE 1: Native Android BarcodeDetector (Chrome ML Kit) on the magnified canvas!
                    if (detector) {
                      try {
                        const nativeResults = await detector.detect(canvas);
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

                    // 2. ENGINE 2: Industrial ZBar WebAssembly with ESL contrast enhancement
                    const imgData = ctx.getImageData(0, 0, targetW, targetH);
                    enhanceESLContrast(imgData.data);

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
                  } catch (_) {
                    // silent frame error
                  } finally {
                    isZBarBusyRef.current = false;
                  }
                }
              }
            }

            animFrameIdRef.current = requestAnimationFrame(detectLoop);
          };

          // Kick off the detection loop immediately
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
      if (focusIntervalRef.current) {
        clearInterval(focusIntervalRef.current);
      }
      if (animFrameIdRef.current) {
        cancelAnimationFrame(animFrameIdRef.current);
      }
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
        streamRef.current = null;
      }
    };
  }, [isOpen, currentCameraIndex]);

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

  // Cycle Zoom (1.0x -> 2.0x -> 3.0x -> 1.0x)
  const cycleZoom = async () => {
    let nextZ = 1.0;
    if (zoomLevel < 1.5) nextZ = 2.0;
    else if (zoomLevel < 2.5) nextZ = 3.0;
    else nextZ = 1.0;

    zoomLevelRef.current = nextZ;
    setZoomLevel(nextZ);

    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ zoom: nextZ }, { focusMode: 'continuous' }]
          });
        }
      }
    } catch (_) {
      try {
        const track = streamRef.current?.getVideoTracks()[0];
        await (track as any)?.applyConstraints({
          advanced: [{ focusMode: 'continuous' }]
        });
      } catch (_) {}
    }
  };

  // Switch physical rear camera lens (e.g. Telephoto optical sensor)
  const cycleCameraLens = () => {
    if (rearCameras.length <= 1) return;
    const nextIndex = (currentCameraIndex + 1) % rearCameras.length;
    setCurrentCameraIndex(nextIndex);
    selectedCameraIdRef.current = rearCameras[nextIndex].deviceId;
  };

  // Tap-to-focus on screen with pointsOfInterest
  const handleScreenTap = async (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    setTapRipple({ x, y });
    setTimeout(() => setTapRipple(null), 600);

    const normX = Math.max(0, Math.min(1, x / rect.width));
    const normY = Math.max(0, Math.min(1, y / rect.height));

    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          try {
            await (track as any).applyConstraints({
              advanced: [
                { pointsOfInterest: [{ x: normX, y: normY }] },
                { focusMode: 'continuous' }
              ]
            });
          } catch (_) {
            await (track as any).applyConstraints({
              advanced: [{ focusMode: 'continuous' }]
            });
          }
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
        enhanceESLContrast(imgData.data);
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
          className="w-full h-full object-cover transition-transform duration-200"
          style={{
            transform: zoomLevel > 1.05 ? `scale(${zoomLevel})` : undefined
          }}
        />
      </div>

      {/* Tap Ripple Indicator */}
      {tapRipple && (
        <div
          className="absolute z-20 pointer-events-none w-14 h-14 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-emerald-400 bg-emerald-400/20 animate-ping"
          style={{ left: tapRipple.x, top: tapRipple.y }}
        />
      )}

      {/* Top Bar with Controls */}
      <div className="relative z-30 flex items-center justify-between p-4 pt-6 sm:p-6 w-full">
        <div className="flex items-center gap-2">
          {/* Flashlight button */}
          {hasTorch && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleTorch();
              }}
              className={`w-11 h-11 rounded-full flex items-center justify-center transition-all ${
                torchOn
                  ? 'bg-amber-400 text-gray-950 shadow-xl shadow-amber-400/50 scale-105'
                  : 'bg-black/60 text-white hover:bg-black/80 backdrop-blur-md'
              }`}
              title="Latarka"
            >
              <Flashlight className="w-5 h-5" />
            </button>
          )}

          {/* Zoom toggle button (1.0x -> 2.0x -> 3.0x) */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              cycleZoom();
            }}
            className="w-11 h-11 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center backdrop-blur-md text-xs font-black shadow-lg border border-white/10"
            title="Przełącz zoom"
          >
            {zoomLevel.toFixed(1)}×
          </button>

          {/* Mode Switcher: Laser ESL vs Standard */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              toggleScanMode();
            }}
            className={`h-11 px-3.5 rounded-full flex items-center gap-1.5 text-xs font-black backdrop-blur-md shadow-lg border transition-all ${
              scanMode === 'laser'
                ? 'bg-rose-600/90 hover:bg-rose-600 text-white border-rose-400/40 shadow-rose-600/30'
                : 'bg-black/60 hover:bg-black/80 text-white border-white/10'
            }`}
            title="Przełącz tryb skanowania"
          >
            <ScanLine className="w-4 h-4" />
            <span>{scanMode === 'laser' ? 'Laser ESL 1.5m' : 'Standard'}</span>
          </button>

          {/* Physical camera switcher if multiple rear cameras detected */}
          {rearCameras.length > 1 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                cycleCameraLens();
              }}
              className="w-11 h-11 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center backdrop-blur-md text-xs font-black shadow-lg border border-white/10"
              title="Przełącz obiektyw tylny"
            >
              <Camera className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Circular Close Button (X) top right */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onClose();
          }}
          className="w-11 h-11 rounded-full bg-black/70 hover:bg-black text-white flex items-center justify-center shadow-lg backdrop-blur-md transition-transform active:scale-95 cursor-pointer border border-white/10"
          title="Zamknij skaner"
        >
          <X className="w-5 h-5 stroke-[2.5]" />
        </button>
      </div>

      {/* Toast message overlay if active */}
      {toastMessage && (
        <div className="absolute top-20 inset-x-4 z-40 flex justify-center pointer-events-none">
          <div className="bg-emerald-600 text-white font-extrabold text-xs sm:text-sm px-5 py-2.5 rounded-full shadow-2xl border border-white/20 animate-in fade-in slide-in-from-top-2">
            {toastMessage}
          </div>
        </div>
      )}

      {/* Central Viewfinder matching chosen mode */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center pointer-events-none p-3">
        {scanMode === 'laser' ? (
          /* PROFESSIONAL ZEBRA / SCANDIT STYLE LASER RETICLE */
          <div
            className={`relative w-[92vw] max-w-[460px] h-[130px] rounded-2xl border-2 transition-all flex items-center justify-center ${
              scannedFeedback
                ? 'border-emerald-400 bg-emerald-500/25 scale-[1.02]'
                : 'border-rose-500/90 shadow-[0_0_25px_rgba(244,63,94,0.4)]'
            }`}
            style={{
              boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.45)'
            }}
          >
            {/* Corner Target Brackets */}
            <div className="absolute -top-1 -left-1 w-6 h-6 border-t-4 border-l-4 border-rose-400 rounded-tl-lg" />
            <div className="absolute -top-1 -right-1 w-6 h-6 border-t-4 border-r-4 border-rose-400 rounded-tr-lg" />
            <div className="absolute -bottom-1 -left-1 w-6 h-6 border-b-4 border-l-4 border-rose-400 rounded-bl-lg" />
            <div className="absolute -bottom-1 -right-1 w-6 h-6 border-b-4 border-r-4 border-rose-400 rounded-br-lg" />

            {/* Glowing Laser Beam Line */}
            <div
              className={`absolute inset-x-2 h-1 rounded-full transition-colors ${
                scannedFeedback
                  ? 'bg-emerald-400 shadow-[0_0_15px_#34d399]'
                  : 'bg-rose-500 shadow-[0_0_12px_#f43f5e] animate-pulse'
              }`}
            />

            {/* Center crosshair dot */}
            <div className="w-2 h-2 rounded-full bg-rose-400 shadow-md" />

            {/* Sub-label hint */}
            <div className="absolute -bottom-7 bg-black/75 px-3 py-1 rounded-full text-[11px] font-bold text-rose-300 border border-rose-500/30 flex items-center gap-1.5 shadow-lg">
              <Crosshair className="w-3.5 h-3.5 text-rose-400" />
              <span>Skieruj laser na kod kreskowy pod ceną</span>
            </div>

            {/* Scan success indicator */}
            {scannedFeedback && (
              <div className="absolute z-20 bg-black/90 text-emerald-400 px-5 py-2.5 rounded-full flex items-center gap-2 font-black text-sm shadow-2xl border border-emerald-400/40">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                <span>Odczytano: {scannedFeedback}</span>
              </div>
            )}
          </div>
        ) : (
          /* STANDARD FULL RETICLE */
          <div
            className={`relative w-[92vw] max-w-[420px] h-[320px] rounded-[36px] border-[3.5px] transition-all flex items-center justify-center ${
              scannedFeedback
                ? 'border-emerald-400 bg-emerald-500/25 scale-[1.02]'
                : 'border-emerald-500'
            }`}
            style={{
              boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.45)'
            }}
          >
            {/* Scan glow */}
            <div className="absolute inset-x-6 top-0 h-1 bg-gradient-to-r from-transparent via-emerald-400 to-transparent animate-pulse rounded-full" />
            <div className="absolute inset-x-6 bottom-0 h-1 bg-gradient-to-r from-transparent via-emerald-400 to-transparent animate-pulse rounded-full" />

            {/* Scan success indicator */}
            {scannedFeedback && (
              <div className="bg-black/90 text-emerald-400 px-5 py-2.5 rounded-full flex items-center gap-2 font-black text-sm shadow-2xl border border-emerald-400/40">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                <span>Odczytano: {scannedFeedback}</span>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Bottom Area with controls */}
      <div className="relative z-30 w-full pb-8 sm:pb-10 pt-3 flex flex-col items-center justify-center gap-3">
        {cameraError && (
          <div className="mx-4 max-w-sm px-4 py-2 bg-black/80 backdrop-blur-md border border-amber-500/50 rounded-xl text-amber-300 text-xs text-center flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>{cameraError}</span>
          </div>
        )}

        <div className="flex items-center gap-2.5">
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

          {/* If continuousMode: [Gotowe / Wróć] button */}
          {continuousMode && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onClose();
              }}
              className="py-2.5 px-5 rounded-2xl bg-white/90 hover:bg-white text-gray-900 font-bold text-sm shadow-xl flex items-center gap-1.5 active:scale-95 transition-all cursor-pointer"
            >
              <span>Wróć do listy</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
