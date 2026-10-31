import { scanImageData, setModuleArgs } from '@undecaf/zbar-wasm';
import { Carpet } from '../types';
import { initialCarpets } from '../data/seedData';

// Configure ZBar WebAssembly path
if (typeof window !== 'undefined') {
  try {
    const baseUrl = import.meta.env.BASE_URL || '/';
    setModuleArgs({
      locateFile: (file: string) => `${baseUrl}${file}`
    });
  } catch (_) {}
}

export interface EslScanResult {
  nazwa: string;
  ref: string;
  ean: string;
  cena?: string;
  zapas?: string;
  source?: 'catalog' | 'barcode' | 'ai';
}

/**
 * Filter valid retail product codes:
 * 13-digit EAN-13, 8-digit REF / EAN-8, 12-digit UPC.
 * Discards tag hardware serials and alphanumeric strings.
 */
export function extractProductCode(raw: string): string | null {
  if (!raw) return null;
  const clean = raw.trim();
  if (/[a-zA-Z]/.test(clean)) return null;
  if (!/^\d+$/.test(clean)) return null;
  if (clean.length === 13 || clean.length === 8 || clean.length === 12) {
    return clean;
  }
  return null;
}

/**
 * Pleasant audio feedback for successful scan
 */
export function playChime() {
  try {
    const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(1760, ctx.currentTime);
    gain.gain.setValueAtTime(0.18, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.12);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.13);
  } catch (_) {}
}

/**
 * Searches the store's active database and fallback catalog for a carpet matching EAN or REF.
 */
export function findCarpetInCatalog(
  code: string,
  carpets: Carpet[] = [],
  fallbackCarpets: Carpet[] = initialCarpets
): Carpet | null {
  const clean = code.trim();
  if (!clean) return null;

  // Search active store inventory first
  const matchActive = carpets.find((c) => {
    const cleanEan = (c.ean || '').trim();
    const cleanRef = (c.ref || '').trim();
    return cleanEan === clean || cleanRef === clean;
  });
  if (matchActive) return matchActive;

  // Search fallback seed catalog (229 store positions)
  const matchFallback = fallbackCarpets.find((c) => {
    const cleanEan = (c.ean || '').trim();
    const cleanRef = (c.ref || '').trim();
    return cleanEan === clean || cleanRef === clean;
  });
  return matchFallback || null;
}

/**
 * Scans an image File locally in the browser using:
 * 1. Native BarcodeDetector (hardware ML vision on Android Chrome)
 * 2. Industrial ZBar WebAssembly
 * 3. High-contrast enhancement pass for e-ink ESL labels
 * 4. 90-degree rotated pass for vertical shelf barcodes
 * Runs 100% locally with 0 latency, 0 external requests, 0 quota.
 */
export async function scanBarcodeFromImageFile(file: File): Promise<string | null> {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onerror = () => resolve(null);
    reader.onload = async (e) => {
      const src = e.target?.result as string;
      if (!src) return resolve(null);

      const img = new Image();
      img.onerror = () => resolve(null);
      img.onload = async () => {
        try {
          const maxDim = 1600;
          let width = img.naturalWidth || img.width;
          let height = img.naturalHeight || img.height;

          if (width > maxDim || height > maxDim) {
            if (width > height) {
              height = Math.round((height * maxDim) / width);
              width = maxDim;
            } else {
              width = Math.round((width * maxDim) / height);
              height = maxDim;
            }
          }

          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          if (!ctx) return resolve(null);

          ctx.drawImage(img, 0, 0, width, height);

          // PASS 1: Native BarcodeDetector API if available
          if (typeof window !== 'undefined' && 'BarcodeDetector' in window) {
            try {
              const detector = new (window as any).BarcodeDetector({
                formats: ['code_128', 'ean_13', 'ean_8', 'upc_a', 'code_39', 'itf']
              });
              const results = await detector.detect(canvas);
              if (results && results.length > 0) {
                for (const r of results) {
                  const valid = extractProductCode(r.rawValue);
                  if (valid) return resolve(valid);
                }
              }
            } catch (_) {}
          }

          // PASS 2: ZBar WASM on normal image
          try {
            const imgData = ctx.getImageData(0, 0, width, height);
            const symbols = await scanImageData(imgData);
            if (symbols && symbols.length > 0) {
              for (const s of symbols) {
                const valid = extractProductCode(s.decode());
                if (valid) return resolve(valid);
              }
            }
          } catch (_) {}

          // PASS 3: ZBar WASM with high-contrast grayscale curve (optimized for e-ink electronic shelf labels)
          try {
            ctx.filter = 'contrast(2.2) brightness(1.1) grayscale(1)';
            ctx.drawImage(img, 0, 0, width, height);
            const enhancedImgData = ctx.getImageData(0, 0, width, height);
            const symbols = await scanImageData(enhancedImgData);
            if (symbols && symbols.length > 0) {
              for (const s of symbols) {
                const valid = extractProductCode(s.decode());
                if (valid) return resolve(valid);
              }
            }
          } catch (_) {}

          // PASS 4: 90-degree rotated canvas (for vertical barcodes on shelf brackets)
          try {
            const rotCanvas = document.createElement('canvas');
            rotCanvas.width = height;
            rotCanvas.height = width;
            const rotCtx = rotCanvas.getContext('2d', { willReadFrequently: true });
            if (rotCtx) {
              rotCtx.translate(height / 2, width / 2);
              rotCtx.rotate((90 * Math.PI) / 180);
              rotCtx.drawImage(canvas, -width / 2, -height / 2);
              const rotImgData = rotCtx.getImageData(0, 0, height, width);
              const symbols = await scanImageData(rotImgData);
              if (symbols && symbols.length > 0) {
                for (const s of symbols) {
                  const valid = extractProductCode(s.decode());
                  if (valid) return resolve(valid);
                }
              }
            }
          } catch (_) {}

          resolve(null);
        } catch (_) {
          resolve(null);
        }
      };
      img.src = src;
    };
    reader.readAsDataURL(file);
  });
}

/**
 * Resizes and compresses an image to optimal size for Gemini OCR.
 */
export async function fileToBase64Compressed(
  file: File,
  maxDim = 1600,
  quality = 0.85
): Promise<{ base64: string; mimeType: string }> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let width = img.width;
        let height = img.height;
        if (width > maxDim || height > maxDim) {
          if (width > height) {
            height = Math.round((height * maxDim) / width);
            width = maxDim;
          } else {
            width = Math.round((width * maxDim) / height);
            height = maxDim;
          }
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve({ base64: e.target?.result as string, mimeType: file.type || 'image/jpeg' });
          return;
        }
        ctx.drawImage(img, 0, 0, width, height);
        const compressedDataUrl = canvas.toDataURL('image/jpeg', quality);
        resolve({ base64: compressedDataUrl, mimeType: 'image/jpeg' });
      };
      img.onerror = reject;
      img.src = e.target?.result as string;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export class EslScanError extends Error {
  isQuotaExceeded: boolean;
  isOverloaded: boolean;

  constructor(message: string, isQuotaExceeded = false, isOverloaded = false) {
    super(message);
    this.name = 'EslScanError';
    this.isQuotaExceeded = isQuotaExceeded;
    this.isOverloaded = isOverloaded;
  }
}

/**
 * Sends image data to server endpoint /api/scan-esl to extract Carpet Name, REF LM, and EAN.
 */
export async function scanEslLabel(
  imageBase64: string,
  mimeType = 'image/jpeg'
): Promise<EslScanResult> {
  const maxRetries = 1;
  let lastError: EslScanError | null = null;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetch('/api/scan-esl', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ imageBase64, mimeType })
      });

      if (response.ok) {
        const data: EslScanResult = await response.json();
        return { ...data, source: 'ai' };
      }

      const errData = await response.json().catch(() => ({}));
      const errMsg = errData.error || `Błąd serwera OCR (${response.status})`;
      const isQuota = Boolean(errData.isQuotaExceeded || response.status === 429);
      const isOverloaded = Boolean(errData.isOverloaded || response.status === 503);

      if (isQuota) {
        throw new EslScanError(errMsg, true, false);
      }

      if (isOverloaded && attempt < maxRetries) {
        await new Promise((r) => setTimeout(r, 1000));
        continue;
      }

      throw new EslScanError(errMsg, isQuota, isOverloaded);
    } catch (err: unknown) {
      if (err instanceof EslScanError) {
        if (err.isQuotaExceeded) {
          throw err;
        }
        lastError = err;
      } else {
        lastError = new EslScanError(err instanceof Error ? err.message : String(err));
      }

      if (attempt < maxRetries && lastError.isOverloaded) {
        await new Promise((r) => setTimeout(r, 1000));
        continue;
      }
      break;
    }
  }

  throw lastError || new EslScanError('Nie udało się odczytać etykiety.');
}

/**
 * Master label scanner:
 * 1. First scans barcode locally using ZBar WASM / BarcodeDetector (instant, offline, 0 quota).
 * 2. If barcode is detected:
 *    - Looks up in store carpets & seed catalog. If found, returns complete carpet data (name, ref, ean) WITHOUT AI!
 *    - If not found in catalog, assigns barcode to EAN/REF and attempts AI OCR in background to read title.
 * 3. If barcode is not detected on photo:
 *    - Attempts AI OCR fallback.
 */
export async function scanLabelSmart(
  file: File,
  carpets: Carpet[] = []
): Promise<EslScanResult> {
  // 1. Try local barcode recognition from the image
  const detectedCode = await scanBarcodeFromImageFile(file);

  if (detectedCode) {
    // 2. Check store catalog for matching carpet
    const match = findCarpetInCatalog(detectedCode, carpets);
    if (match) {
      playChime();
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        try {
          navigator.vibrate([60, 40, 60]);
        } catch (_) {}
      }
      return {
        nazwa: match.nazwa,
        ref: match.ref || (detectedCode.length === 8 ? detectedCode : ''),
        ean: match.ean || (detectedCode.length === 13 ? detectedCode : detectedCode),
        source: 'catalog'
      };
    }

    // Code found, but not in existing catalog -> set EAN/REF and try AI for title if possible
    let aiName = '';
    let aiRef = detectedCode.length === 8 ? detectedCode : '';
    let aiEan = detectedCode.length === 13 ? detectedCode : detectedCode;

    try {
      const { base64, mimeType } = await fileToBase64Compressed(file);
      const aiResult = await scanEslLabel(base64, mimeType);
      if (aiResult.nazwa) aiName = aiResult.nazwa;
      if (aiResult.ref) aiRef = aiResult.ref;
      if (aiResult.ean) aiEan = aiResult.ean;
      return {
        nazwa: aiName,
        ref: aiRef,
        ean: aiEan,
        source: 'ai'
      };
    } catch (_) {
      // AI unavailable or quota exhausted — that's completely fine, we have the barcode!
      playChime();
      return {
        nazwa: '',
        ref: aiRef,
        ean: aiEan,
        source: 'barcode'
      };
    }
  }

  // No barcode found on image -> fallback to AI OCR
  const { base64, mimeType } = await fileToBase64Compressed(file);
  const aiResult = await scanEslLabel(base64, mimeType);
  playChime();
  return {
    ...aiResult,
    source: 'ai'
  };
}
