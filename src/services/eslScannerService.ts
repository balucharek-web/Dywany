export interface EslScanResult {
  nazwa: string;
  ref: string;
  ean: string;
  cena?: string;
  zapas?: string;
}

/**
 * Resizes and compresses an image to optimal size for Gemini OCR.
 * Keeps latency low and bandwidth usage minimal on warehouse Wi-Fi/LTE.
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

/**
 * Sends image data to server endpoint /api/scan-esl to extract Carpet Name, REF LM, and EAN.
 * Includes automatic client-side retry for transient 503 server demand spikes.
 */
export async function scanEslLabel(
  imageBase64: string,
  mimeType = 'image/jpeg'
): Promise<EslScanResult> {
  const maxRetries = 2;
  let lastError: Error | null = null;

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
        return data;
      }

      const errData = await response.json().catch(() => ({}));
      const errMsg = errData.error || `Błąd serwera OCR (${response.status})`;

      // If it's a 503/high-demand error and we have retries left, wait and retry
      if ((response.status === 503 || errMsg.includes('503') || errMsg.includes('przeciążenie')) && attempt < maxRetries) {
        await new Promise((r) => setTimeout(r, 1200 * (attempt + 1)));
        continue;
      }

      throw new Error(errMsg);
    } catch (err: unknown) {
      lastError = err instanceof Error ? err : new Error(String(err));
      if (attempt < maxRetries && lastError.message.includes('503')) {
        await new Promise((r) => setTimeout(r, 1200 * (attempt + 1)));
        continue;
      }
      break;
    }
  }

  throw lastError || new Error('Nie udało się odczytać etykiety.');
}
