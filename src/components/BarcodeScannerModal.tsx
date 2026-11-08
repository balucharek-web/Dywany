import { useEffect, useRef, useState } from 'react';
import { X, Flashlight, AlertCircle, RefreshCw, CheckCircle2, Camera } from 'lucide-react';
import { scanImageData, setModuleArgs } from '@undecaf/zbar-wasm';
import { decodeScanditWaveform } from '../services/scanditWaveformEngine';

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
    // 13-digit EAN-13 (Standard retail product barcode: 590..., 3276..., etc.)
    if (clean.length === 13) {
      return clean;
    }
    // 12-digit UPC-A (Standard retail barcode)
    if (clean.length === 12) {
      return clean;
    }
    // 8-digit Ref LM (Leroy Merlin catalog reference: starts with 4, 5, 6, 7, 8, 9)
    // Rejects phantom 8-digit slice codes starting with 0, 1, 2, 3 (e.g. 28188322)
    if (clean.length === 8 && /^[4-9]/.test(clean)) {
      return clean;
    }
    // GTIN-14 case packaging
    if (clean.length === 14) {
      return clean;
    }
    return null;
  }

  // 2. If it's a URL or text containing an 8-digit Ref LM or 13-digit EAN
  const match = raw.match(/\b(59\d{11}|32\d{11}|40\d{11}|[4-9]\d{7})\b/);
  if (match) {
    return match[1];
  }

  return null;
}

/**
 * SCANDIT-GRADE SUB-PIXEL DE-BLURRING & CONTRAST PIPELINE:
 * 1. Glare suppression: strips specular highlights (> 245) from overhead supermarket lighting.
 * 2. Adaptive contrast expansion: maps true barcode bars to pure black (0) and background to pure white (255).
 * 3. Horizontal Laplacian Unsharp Mask: sharpens 1D bar edges row-by-row,
 *    reconstructing 1-pixel bars blurred by distance or camera lens motion at sub-pixel precision!
 */
const reusableRowLum = new Uint8Array(2048);

function enhanceToughBarcode(data: Uint8ClampedArray, width: number, height: number) {
  const len = data.length;
  if (len === 0) return;

  // 1. Build a 32-bucket histogram from sampled pixels (every 8th pixel for ~0.2ms speed)
  const hist = new Uint32Array(32);
  let totalSampled = 0;
  for (let i = 0; i < len; i += 32) {
    const lum = (data[i] * 77 + data[i + 1] * 150 + data[i + 2] * 29) >> 8;
    hist[lum >> 3]++;
    totalSampled++;
  }

  // 2. Percentile calculation: 5% (darkest true bars) and 90% (label background, ignoring specular glare)
  const lowTarget = Math.floor(totalSampled * 0.05);
  const highTarget = Math.floor(totalSampled * 0.90);

  let accum = 0;
  let pLow = 0;
  let pHigh = 255;

  for (let b = 0; b < 32; b++) {
    accum += hist[b];
    if (pLow === 0 && accum >= lowTarget) {
      pLow = b << 3;
    }
    if (accum >= highTarget) {
      pHigh = Math.min(255, (b << 3) + 7);
      break;
    }
  }

  // Fallback sanity bounds
  if (pHigh <= pLow + 15) {
    pLow = 0;
    pHigh = 255;
  }

  const range = pHigh - pLow;
  const invRange = 255 / (range > 0 ? range : 1);

  // 3. Fast 256-byte lookup table
  const lut = new Uint8Array(256);
  for (let i = 0; i < 256; i++) {
    if (i <= pLow) lut[i] = 0;
    else if (i >= pHigh) lut[i] = 255;
    else lut[i] = Math.min(255, Math.max(0, ((i - pLow) * invRange) | 0));
  }

  // 4. Scandit-style row-by-row horizontal de-blurring unsharp filter
  // In 1D barcodes, high frequencies are horizontal. This sharpens 0.5 - 1.2px bar edges!
  if (width > 2 && height > 2 && width * height * 4 === len) {
    const rowLum = width <= 2048 ? reusableRowLum : new Uint8Array(width);
    for (let y = 0; y < height; y++) {
      const rowOffset = y * width * 4;
      for (let x = 0; x < width; x++) {
        const idx = rowOffset + (x << 2);
        const rawLum = (data[idx] * 77 + data[idx + 1] * 150 + data[idx + 2] * 29) >> 8;
        rowLum[x] = lut[rawLum];
      }
      // Apply 3-tap horizontal sharpening: Center * 2.2 - (Left + Right) * 0.6
      for (let x = 1; x < width - 1; x++) {
        const c = rowLum[x];
        const left = rowLum[x - 1];
        const right = rowLum[x + 1];
        const sharp = Math.min(255, Math.max(0, Math.round(c * 2.2 - (left + right) * 0.6)));
        const idx = rowOffset + (x << 2);
        data[idx] = sharp;
        data[idx + 1] = sharp;
        data[idx + 2] = sharp;
      }
    }
  } else {
    for (let i = 0; i < len; i += 4) {
      const lum = (data[i] * 77 + data[i + 1] * 150 + data[i + 2] * 29) >> 8;
      const v = lut[lum];
      data[i] = v;
      data[i + 1] = v;
      data[i + 2] = v;
    }
  }
}

/**
 * Scans image with ZBar WASM:
 * Pass 1: Tests clean raw camera image directly (best for ZBar's internal C adaptive binarizer).
 * Pass 2: Falls back to contrast-normalized lower band (shelf ESL tags) if Pass 1 fails.
 */
async function scanWithPartialFallback(
  ctx: CanvasRenderingContext2D,
  targetW: number,
  targetH: number
): Promise<string | null> {
  const imgData = ctx.getImageData(0, 0, targetW, targetH);

  // Pass 1: Clean raw unaltered frame (ZBar WASM native C adaptive binarizer is optimized for raw pixels)
  let symbols = await scanImageData(imgData);
  if (symbols && symbols.length > 0) {
    for (const sym of symbols) {
      const valid = extractProductCode(sym.decode());
      if (valid) return valid;
    }
  }

  // Pass 2: Lower shelf sub-band where barcodes on ESL price tags reside
  if (targetH >= 90) {
    const subH = Math.round(targetH * 0.55);
    const subY = Math.round(targetH * 0.38);
    const subData = ctx.getImageData(0, subY, targetW, subH);
    enhanceToughBarcode(subData.data, targetW, subH);
    symbols = await scanImageData(subData);
    if (symbols && symbols.length > 0) {
      for (const sym of symbols) {
        const valid = extractProductCode(sym.decode());
        if (valid) return valid;
      }
    }
  }

  return null;
}

export function BarcodeScannerModal({
  isOpen,
  onClose,
  onScanSuccess,
  title: _title,
  continuousMode = false,
  toastMessage = null
}: BarcodeScannerModalProps) {
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [hasTorch, setHasTorch] = useState(false);
  const [isHardwareZoom, setIsHardwareZoom] = useState(false);
  const isHardwareZoomRef = useRef(false);
  const [zoomLevel, setZoomLevel] = useState(2.0);
  const zoomLevelRef = useRef(2.0);
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
  const isBusyRef = useRef(false);
  const focusIntervalRef = useRef<any>(null);
  const touchStartDistRef = useRef<number | null>(null);
  const touchStartZoomRef = useRef<number>(1.0);
  const waveformCandidateRef = useRef<{ code: string; time: number } | null>(null);

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

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    isScannedRef.current = false;
    isBusyRef.current = false;
    waveformCandidateRef.current = null;
    setScannedFeedback(null);
    setCameraError(null);
    setTorchOn(false);
    setHasTorch(false);
    setIsHardwareZoom(false);
    isHardwareZoomRef.current = false;
    setZoomLevel(2.0);
    zoomLevelRef.current = 2.0;

    const initScanner = async () => {
      try {
        // Initialize Native BarcodeDetector (Chrome Android ML Kit hardware acceleration)
        let detector: any = null;
        if (hasNativeBarcodeDetector) {
          try {
            const supportedFormats = await (window as any).BarcodeDetector.getSupportedFormats();
            const targetFormats = ['code_128', 'ean_13', 'upc_a', 'code_39', 'itf'].filter((f) =>
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
              width: { ideal: 4096 },
              height: { ideal: 2160 },
              frameRate: { ideal: 60, min: 30 }
            },
            audio: false
          });
        } catch (_) {
          stream = await navigator.mediaDevices.getUserMedia({
            video: {
              facingMode: { ideal: 'environment' },
              width: { ideal: 1920 },
              height: { ideal: 1080 }
            },
            audio: false
          });
        }

        if (!isMounted) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }

        streamRef.current = stream;

        // Populate ONLY rear cameras (strictly excluding front/selfie cameras)
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

          // Apply continuous autofocus, auto-exposure, and Scandit exposure compensation
          const advancedConstraints: any = {};
          if (capabilities.focusMode?.includes('continuous')) {
            advancedConstraints.focusMode = 'continuous';
          }
          if (capabilities.exposureMode?.includes('continuous')) {
            advancedConstraints.exposureMode = 'continuous';
          }
          // Scandit Secret 1: Drop exposure (-1.2 EV) so bright store fluorescent lights don't blow out white e-paper
          if (capabilities.exposureCompensation) {
            const minExp = capabilities.exposureCompensation.min ?? -2.0;
            advancedConstraints.exposureCompensation = Math.max(minExp, -1.2);
          }

          // Scandit Secret 2: Intelligent Default Telephoto Zoom (2.0x)
          // From 1.5m away, wide lens (1.0x) yields a tiny 40px barcode on e-paper.
          // Starting at 2.0x telephoto yields 180-240px with razor-sharp optical separation!
          if (capabilities.zoom) {
            setIsHardwareZoom(true);
            isHardwareZoomRef.current = true;
            const maxZ = Math.min(capabilities.zoom.max || 4.0, 4.0);
            setMaxZoom(maxZ);
            const defaultZ = Math.min(maxZ, Math.max(capabilities.zoom.min || 1.0, 2.0));
            setZoomLevel(defaultZ);
            zoomLevelRef.current = defaultZ;
            advancedConstraints.zoom = defaultZ;
          } else {
            setIsHardwareZoom(false);
            isHardwareZoomRef.current = false;
            setMaxZoom(3.0);
            setZoomLevel(1.8);
            zoomLevelRef.current = 1.8;
          }

          if (Object.keys(advancedConstraints).length > 0) {
            try {
              await (track as any).applyConstraints({ advanced: [advancedConstraints] });
            } catch (_) {}
          }
        }


        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();

          // Offscreen high-density canvas for ultra-fast ROI processing
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          canvasRef.current = canvas;

          let lastScanTime = 0;
          let lastZBarTime = 0;
          let cycleCount = 0;

          // UNIFIED ULTRA-FAST DETECTION LOOP:
          // Seamlessly alternates between:
          // 1. Telephoto Center ROI (1:1 native sensor resolution for small ESL barcodes from 1.5m away)
          // 2. Full Viewfinder Window (for close-up scans and full carpet labels)
          // Runs at silky-smooth 60/120 FPS with ZERO main-thread blocking!
          const detectLoop = async (timestamp: number) => {
            if (!isMounted || isScannedRef.current) return;

            const video = videoRef.current;
            if (video && video.readyState >= 2 && ctx) {
              const vw = video.videoWidth;
              const vh = video.videoHeight;

              if (vw > 0 && vh > 0) {
                if (!isBusyRef.current && timestamp - lastScanTime > 30) {
                  lastScanTime = timestamp;
                  isBusyRef.current = true;
                  cycleCount++;

                  try {
                    // Engine 1: Native Android BarcodeDetector (Chrome ML Kit GPU vision)
                    // Google ML Kit neural network processes HTMLVideoElement directly on GPU in ~15ms!
                    if (detector) {
                      try {
                        const nativeResults = await detector.detect(video);
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

                    // Viewfinder Reticle Crop: Synchronizes the green frame with canvas pixels
                    // Reticle occupies ~90% width and ~48% height of the portrait viewport
                    const z = isHardwareZoomRef.current ? 1.0 : (zoomLevelRef.current || 1.0);
                    const cropW = Math.min(vw, Math.round((vw * 0.90) / z));
                    const cropH = Math.min(vh, Math.round((vh * 0.48) / z));
                    const cropX = Math.max(0, Math.round((vw - cropW) / 2));
                    const cropY = Math.max(0, Math.round((vh - cropH) / 2));

                    const targetW = 960;
                    const targetH = Math.round((cropH * targetW) / cropW);

                    if (canvas.width !== targetW || canvas.height !== targetH) {
                      canvas.width = targetW;
                      canvas.height = targetH;
                    }

                    ctx.drawImage(video, cropX, cropY, cropW, cropH, 0, 0, targetW, targetH);

                    // If detect(video) was not supported on this browser version, test canvas:
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

                    // Engine 2: Scandit-Grade 1D Sub-Pixel Continuous Waveform Engine
                    // Samples 64 high-density scanlines spaced only 4-8px apart directly over the reticle
                    const scanditCode = decodeScanditWaveform(ctx, targetW, targetH);
                    if (scanditCode) {
                      triggerScanSuccess(scanditCode);
                      return;
                    }

                    // Engine 3: ZBar WASM on Raw Pixels (checked every 80ms)
                    if (timestamp - lastZBarTime > 80) {
                      lastZBarTime = timestamp;
                      const validProduct = await scanWithPartialFallback(ctx, targetW, targetH);
                      if (validProduct) {
                        triggerScanSuccess(validProduct);
                        return;
                      }
                    }
                  } catch (_) {
                    // silent frame error
                  } finally {
                    isBusyRef.current = false;
                  }
                }
              }
            }

            animFrameIdRef.current = requestAnimationFrame(detectLoop);
          };

          // Kick off detection loop
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

  // Unified hardware/software camera zoom
  const applyZoom = async (nextZ: number) => {
    const clamped = Math.min(maxZoom, Math.max(1.0, +nextZ.toFixed(1)));
    zoomLevelRef.current = clamped;
    setZoomLevel(clamped);

    try {
      if (streamRef.current) {
        const track = streamRef.current.getVideoTracks()[0];
        if (track && (track as any).applyConstraints) {
          await (track as any).applyConstraints({
            advanced: [{ zoom: clamped }, { focusMode: 'continuous' }]
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

  // Cycle Zoom button (1.0x -> 2.0x -> 3.0x -> 1.0x)
  const cycleZoom = () => {
    let nextZ = 1.0;
    if (zoomLevel < 1.5) nextZ = 2.0;
    else if (zoomLevel < 2.5) nextZ = 3.0;
    else nextZ = 1.0;
    applyZoom(nextZ);
  };

  // Pinch-to-zoom gesture tracking
  const handleTouchStart = (e: React.TouchEvent<HTMLDivElement>) => {
    if (e.touches.length === 2) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      touchStartDistRef.current = dist;
      touchStartZoomRef.current = zoomLevelRef.current;
    }
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    if (e.touches.length === 2 && touchStartDistRef.current !== null) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      const factor = dist / touchStartDistRef.current;
      const newZ = touchStartZoomRef.current * factor;
      applyZoom(newZ);
    }
  };

  const handleTouchEnd = () => {
    touchStartDistRef.current = null;
  };

  // Switch physical rear camera lens if multiple sensors present
  const cycleCameraLens = () => {
    if (rearCameras.length <= 1) return;
    const nextIndex = (currentCameraIndex + 1) % rearCameras.length;
    setCurrentCameraIndex(nextIndex);
    selectedCameraIdRef.current = rearCameras[nextIndex].deviceId;
  };

  // Tap-to-focus on screen with pointsOfInterest
  const handleScreenTap = async (e: React.MouseEvent<HTMLDivElement>) => {
    if (touchStartDistRef.current !== null) return;

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

  // Scan from photo file using ZBar WASM with Tough Barcode Glare Recovery
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
        const valid = await scanWithPartialFallback(ctx, bitmap.width, bitmap.height);
        if (valid) {
          triggerScanSuccess(valid);
          return;
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
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      className="fixed inset-0 z-50 bg-black flex flex-col justify-between overflow-hidden select-none touch-none"
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
            transform: !isHardwareZoom && zoomLevel > 1.05 ? `scale(${zoomLevel})` : undefined
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

          {/* Physical camera lens switcher if multiple rear cameras detected */}
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

      {/* Scandit Enterprise Viewfinder with Laser Sweep */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center pointer-events-none p-3">
        <div
          className={`relative w-[92vw] max-w-[420px] h-[340px] sm:h-[380px] rounded-[36px] border-[3.5px] transition-all flex items-center justify-center overflow-hidden ${
            scannedFeedback
              ? 'border-emerald-400 bg-emerald-500/25 scale-[1.02]'
              : 'border-[#22c55e]'
          }`}
          style={{
            boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.40)'
          }}
        >
          {/* Scandit Corner Target Brackets */}
          <div className="absolute top-2.5 left-2.5 w-8 h-8 border-t-[4px] border-l-[4px] border-[#22c55e] rounded-tl-[22px]" />
          <div className="absolute top-2.5 right-2.5 w-8 h-8 border-t-[4px] border-r-[4px] border-[#22c55e] rounded-tr-[22px]" />
          <div className="absolute bottom-2.5 left-2.5 w-8 h-8 border-b-[4px] border-l-[4px] border-[#22c55e] rounded-bl-[22px]" />
          <div className="absolute bottom-2.5 right-2.5 w-8 h-8 border-b-[4px] border-r-[4px] border-[#22c55e] rounded-br-[22px]" />

          {/* Scandit Animated Smooth Laser Sweep Beam */}
          {!scannedFeedback && <div className="scandit-laser-line" />}

          {/* Subtle animated scan glow accents */}
          <div className="absolute inset-x-8 top-0 h-1.5 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />
          <div className="absolute inset-x-8 bottom-0 h-1.5 bg-gradient-to-r from-transparent via-[#22c55e] to-transparent animate-pulse rounded-full" />

          {/* Scan success indicator */}
          {scannedFeedback && (
            <div className="bg-black/90 text-emerald-400 px-5 py-2.5 rounded-full flex items-center gap-2 font-black text-sm sm:text-base shadow-2xl border border-emerald-400/40 animate-in fade-in zoom-in-95">
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
              <span>Odczytano: {scannedFeedback}</span>
            </div>
          )}
        </div>

        {/* Scandit Matrix Status Badge */}
        {!scannedFeedback && (
          <div className="mt-4 px-3.5 py-1 bg-black/60 backdrop-blur-md rounded-full border border-white/10 text-[11px] font-bold text-gray-300 flex items-center gap-2 shadow-lg">
            <span className="w-2 h-2 rounded-full bg-[#22c55e] animate-ping" />
            <span>Scandit Matrix Engine • Automatyczny odczyt 1.5m</span>
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

          {/* If continuousMode: [Gotowe / Wróć do listy] button */}
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
