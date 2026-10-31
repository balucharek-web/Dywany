import { createWorker } from 'tesseract.js';
import { scanImageData } from '@undecaf/zbar-wasm';

export interface LocalOcrResult {
  nazwa: string;
  ref: string;
  ean: string;
  cena?: string;
  rawText?: string;
}

/**
 * Normalizes common OCR misread digits for 8-digit LM reference numbers
 */
function fixRefDigits(text: string): string {
  return text
    .replace(/[oO]/g, '0')
    .replace(/[iIl|]/g, '1')
    .replace(/[sS]/g, '5')
    .replace(/[bB]/g, '8')
    .replace(/\s+/g, '');
}

/**
 * Pre-processes an image to high contrast black and white for superior e-ink text recognition
 */
export async function preprocessImageForOcr(
  imageSource: CanvasImageSource,
  width: number,
  height: number
): Promise<{ processedDataUrl: string; barcodeFound: string | null }> {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) throw new Error('Nie można utworzyć kontekstu 2D');

  ctx.drawImage(imageSource, 0, 0, width, height);

  // 1. Try local barcode detection on raw image first
  let barcodeFound: string | null = null;
  try {
    const rawData = ctx.getImageData(0, 0, width, height);
    const symbols = await scanImageData(rawData);
    if (symbols && symbols.length > 0) {
      for (const s of symbols) {
        const decoded = s.decode().trim();
        if (/^\d{8}$/.test(decoded) || /^\d{12,13}$/.test(decoded)) {
          barcodeFound = decoded;
          break;
        }
      }
    }
  } catch (_) {}

  // 2. Apply grayscale and contrast enhancement for text OCR
  const imgData = ctx.getImageData(0, 0, width, height);
  const data = imgData.data;
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    // Luminance
    let gray = 0.299 * r + 0.587 * g + 0.114 * b;

    // High contrast curve for e-ink (stretch black & white)
    if (gray < 115) {
      gray = Math.max(0, gray * 0.4); // make darks crisp black
    } else {
      gray = Math.min(255, gray * 1.25); // make lights crisp white
    }

    data[i] = gray;
    data[i + 1] = gray;
    data[i + 2] = gray;
  }
  ctx.putImageData(imgData, 0, 0);

  // 3. Check barcode again on high-contrast if not found initially
  if (!barcodeFound) {
    try {
      const symbols2 = await scanImageData(imgData);
      if (symbols2 && symbols2.length > 0) {
        for (const s of symbols2) {
          const decoded = s.decode().trim();
          if (/^\d{8}$/.test(decoded) || /^\d{12,13}$/.test(decoded)) {
            barcodeFound = decoded;
            break;
          }
        }
      }
    } catch (_) {}
  }

  return {
    processedDataUrl: canvas.toDataURL('image/jpeg', 0.92),
    barcodeFound
  };
}

// Singleton worker promise for fast reuse across multiple scans
let workerPromise: Promise<any> | null = null;

async function getOcrWorker(onProgress?: (progress: number) => void) {
  if (!workerPromise) {
    workerPromise = (async () => {
      const worker = await createWorker('pol+eng', 1, {
        logger: (m) => {
          if (m.status === 'recognizing text' && onProgress && typeof m.progress === 'number') {
            onProgress(Math.round(m.progress * 100));
          }
        }
      });
      return worker;
    })();
  }
  return workerPromise;
}

/**
 * Extracts carpet product data from an ESL label image completely locally without AI
 */
export async function scanEslLabelWithoutAi(
  imageSource: CanvasImageSource | string,
  width?: number,
  height?: number,
  onProgress?: (pct: number) => void
): Promise<LocalOcrResult> {
  let targetImage: string | HTMLCanvasElement;
  let barcodeDetected: string | null = null;

  if (typeof imageSource === 'string') {
    // base64 string or URL
    const img = new Image();
    img.crossOrigin = 'anonymous';
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve();
      img.onerror = () => reject(new Error('Nie można załadować obrazu'));
      img.src = imageSource;
    });

    const w = width || img.naturalWidth || 1280;
    const h = height || img.naturalHeight || 720;
    const prep = await preprocessImageForOcr(img, w, h);
    targetImage = prep.processedDataUrl;
    barcodeDetected = prep.barcodeFound;
  } else {
    const w = width || 1280;
    const h = height || 720;
    const prep = await preprocessImageForOcr(imageSource, w, h);
    targetImage = prep.processedDataUrl;
    barcodeDetected = prep.barcodeFound;
  }

  if (onProgress) onProgress(15);

  const worker = await getOcrWorker(onProgress);

  const ocrResult = await worker.recognize(targetImage);
  const text = ocrResult.data.text || '';

  if (onProgress) onProgress(100);

  const lines = text
    .split('\n')
    .map((l: string) => l.trim())
    .filter((l: string) => l.length > 0);

  let nazwa = '';
  let ref = '';
  let ean = '';
  let cena = '';

  // 1. Process Barcode found via ZBar
  if (barcodeDetected) {
    if (barcodeDetected.length === 13) {
      ean = barcodeDetected;
    } else if (barcodeDetected.length === 8) {
      ref = barcodeDetected;
    }
  }

  // 2. Parse text lines
  const potentialNameLines: string[] = [];

  for (const rawLine of lines) {
    const lineUpper = rawLine.toUpperCase();

    // Check for 13-digit EAN (e.g. 3276007651843)
    if (!ean) {
      const eanMatch = rawLine.match(/\b(3\d{12}|590\d{10}|\d{13})\b/);
      if (eanMatch) {
        ean = eanMatch[1];
      }
    }

    // Check for 8-digit REF LM (e.g. 87797153 or 92116369)
    if (!ref) {
      // Look for standalone 8 digits or next to REF
      const refMatch = rawLine.match(/\b([89]\d{7})\b/);
      if (refMatch) {
        ref = refMatch[1];
      } else {
        // Test near REF/ART label
        const refNear = rawLine.match(/(?:REF|ART|LM|NR)\.?\s*[:\-\s]?\s*([0-9a-zA-Z]{8})/i);
        if (refNear) {
          const cleaned = fixRefDigits(refNear[1]);
          if (/^\d{8}$/.test(cleaned)) {
            ref = cleaned;
          }
        }
      }
    }

    // Check for Price (e.g. 699,00 or 189.00 or large numbers)
    if (!cena) {
      const cenaMatch = rawLine.match(/\b(\d{1,4}[,\.]\d{2})\s*(?:ZŁ|ZL|PLN)?/i);
      if (cenaMatch) {
        cena = cenaMatch[1].replace('.', ',');
      }
    }

    // Check for Carpet Name candidates
    // Look for lines containing "DYWAN", "CHODNIK", dimension patterns like 160X230, or uppercase words
    const isNoise =
      /ZAPAS|SZTUK|LEROY|MERLIN|CENA|KOD|EAN|REF|VAT|SKLEP|WARUNKI|GWARANCJA/i.test(lineUpper);

    if (!isNoise) {
      if (
        lineUpper.includes('DYWAN') ||
        lineUpper.includes('CHODNIK') ||
        lineUpper.includes('WYKŁADZINA') ||
        /\b\d{2,3}\s*[X*x]\s*\d{2,3}\b/.test(lineUpper)
      ) {
        potentialNameLines.push(rawLine);
      } else if (rawLine.length > 5 && /^[A-Z0-9\s\-_.,/]+$/.test(rawLine) && !/^\d+$/.test(rawLine)) {
        potentialNameLines.push(rawLine);
      }
    }
  }

  // If we found specific carpet lines, combine them or take the most relevant
  if (potentialNameLines.length > 0) {
    // Prioritize line containing DYWAN or dimensions
    const mainLine =
      potentialNameLines.find(
        (l) => l.toUpperCase().includes('DYWAN') || /\d{2,3}\s*[X*x]\s*\d{2,3}/.test(l)
      ) || potentialNameLines[0];

    // Clean up unwanted noise characters
    nazwa = mainLine
      .replace(/[^\w\sĄĆĘŁŃÓŚŹŻąćęłńóśźżX*-]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .toUpperCase();
  }

  // Fallback: If ref was not found, check any 8 digit number in all lines
  if (!ref) {
    const allText = lines.join(' ');
    const general8 = allText.match(/\b\d{8}\b/);
    if (general8) {
      ref = general8[0];
    }
  }

  // Fallback: If ean was not found, check any 13 digit number in all lines
  if (!ean) {
    const allText = lines.join(' ');
    const general13 = allText.match(/\b\d{13}\b/);
    if (general13) {
      ean = general13[0];
    }
  }

  return {
    nazwa,
    ref,
    ean,
    cena,
    rawText: text
  };
}
