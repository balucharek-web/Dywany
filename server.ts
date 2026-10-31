import express from 'express';
import { GoogleGenAI, Type, ThinkingLevel } from '@google/genai';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  const app = express();
  const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

  // JSON body parser with 25MB limit for high-res camera captures
  app.use(express.json({ limit: '25mb' }));

  const ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build'
      }
    }
  });

  function parseErrorInfo(err: any): { message: string; isQuota: boolean; isOverloaded: boolean } {
    if (!err) {
      return { message: 'Nieznany błąd podczas odczytu etykiety', isQuota: false, isOverloaded: false };
    }

    const rawMsg = typeof err === 'string' ? err : (err.message || JSON.stringify(err));
    let parsed: any = null;

    try {
      const match = rawMsg.match(/\{[\s\S]*\}/);
      if (match) {
        parsed = JSON.parse(match[0]);
      }
    } catch (_) {}

    const code = parsed?.error?.code || (err as any)?.status;
    const status = parsed?.error?.status || '';
    const desc = parsed?.error?.message || rawMsg;

    // Daily quota exhaustion or rate limit (HTTP 429)
    if (
      code === 429 ||
      status === 'RESOURCE_EXHAUSTED' ||
      desc.includes('RESOURCE_EXHAUSTED') ||
      desc.includes('Quota exceeded') ||
      desc.includes('free_tier_requests')
    ) {
      return {
        message: 'Dzienny limit bezpłatnych zapytań AI (20 zapytań/dzień na darmowym planie Google) został osiągnięty. Możesz wprowadzić dywan ręcznie, zeskanować kod EAN aparatem lub podpiąć klucz z płatnym planem (Pay-as-you-go) w panelu Google AI Studio (Settings > Secrets).',
        isQuota: true,
        isOverloaded: false
      };
    }

    // High demand / service unavailable (HTTP 503)
    if (
      code === 503 ||
      status === 'UNAVAILABLE' ||
      desc.includes('high demand') ||
      desc.includes('UNAVAILABLE')
    ) {
      return {
        message: 'Serwery Google AI mają chwilowe przeciążenie (503). Spróbuj ponownie za chwilę lub wprowadź dane ręcznie.',
        isQuota: false,
        isOverloaded: true
      };
    }

    if (parsed?.error?.message) {
      return {
        message: parsed.error.message,
        isQuota: false,
        isOverloaded: false
      };
    }

    return {
      message: rawMsg,
      isQuota: false,
      isOverloaded: false
    };
  }

  // ESL scanner OCR endpoint with automatic model resilience & quota detection
  app.post('/api/scan-esl', async (req, res) => {
    try {
      const { imageBase64, mimeType = 'image/jpeg' } = req.body;
      if (!imageBase64) {
        return res.status(400).json({ error: 'Brak danych zdjęcia etykiety' });
      }

      const cleanBase64 = imageBase64.replace(/^data:image\/[a-zA-Z0-9+.-]+;base64,/, '');

      const promptText = `Jesteś precyzyjnym systemem OCR dla sklepu Leroy Merlin.
Na zdjęciu znajduje się elektroniczna etykieta cenowa (ESL / cenówka e-ink) z dywanu.
Twoim celem jest bezbłędne odczytanie:
1. "nazwa": Pełna nazwa dywanu wraz z wymiarami i kolorem (np. "DYWAN BLIDA 160X230CM SZARY SMOKE", "DYWAN AMBA 01 160X220 BEZ", "DYWAN SPARKLES BALTA 160X230CM CREAM 5", "DYWAN PACYFIK 160X230 NIEB-SZARY 122"). Zachowaj wielkie litery i wymiary tak jak na etykiecie.
2. "ref": 8-cyfrowy numer referencyjny Leroy Merlin (REF. LM) pod nazwą (np. "87797153", "96821522", "92116398", "82850488", "91659479").
3. "ean": 13-cyfrowy kod kreskowy EAN (np. "3276007651843", "5905549008430", "3276007888560", "9990001755256", "5907603499093") wydrukowany na dole etykiety obok lub nad kodem kreskowym (odrzuć ewentualne litery typu APS na końcu, zostaw tylko 13 cyfr kodu kreskowego).
4. "cena": Cena widoczna na etykiecie (np. "699,00", "469,00").
5. "zapas": Liczba zapasu jeśli widoczna (np. "0", "3").

Zwróć wynik jako JSON z polami: nazwa, ref, ean, cena, zapas.`;

      // Candidate models for OCR
      const candidateConfigs: Array<{ model: string; thinkingConfig?: any }> = [
        { model: 'gemini-3.8-flash', thinkingConfig: { thinkingLevel: ThinkingLevel.LOW } },
        { model: 'gemini-3.1-flash-lite' },
        { model: 'gemini-flash-latest' }
      ];

      let lastError: any = null;
      let textResult: string | null = null;
      let quotaExceededDetected = false;

      for (const config of candidateConfigs) {
        // If daily quota was hit on one of the models, do not keep spamming requests
        if (quotaExceededDetected) break;

        for (let attempt = 0; attempt < 2; attempt++) {
          try {
            const response = await ai.models.generateContent({
              model: config.model,
              contents: [
                {
                  role: 'user',
                  parts: [
                    {
                      inlineData: {
                        data: cleanBase64,
                        mimeType
                      }
                    },
                    {
                      text: promptText
                    }
                  ]
                }
              ],
              config: {
                responseMimeType: 'application/json',
                ...(config.thinkingConfig ? { thinkingConfig: config.thinkingConfig } : {}),
                responseSchema: {
                  type: Type.OBJECT,
                  properties: {
                    nazwa: { type: Type.STRING },
                    ref: { type: Type.STRING },
                    ean: { type: Type.STRING },
                    cena: { type: Type.STRING },
                    zapas: { type: Type.STRING }
                  },
                  required: ['nazwa', 'ref', 'ean']
                }
              }
            });

            if (response.text) {
              textResult = response.text;
              break;
            }
          } catch (err: any) {
            lastError = err;
            const errInfo = parseErrorInfo(err);
            console.warn(`Model ${config.model} attempt ${attempt + 1} failed:`, errInfo.message);

            if (errInfo.isQuota) {
              quotaExceededDetected = true;
              break;
            }

            // Wait briefly before retry if 503
            if (attempt === 0) {
              await new Promise((r) => setTimeout(r, 600));
            }
          }
        }
        if (textResult) break;
      }

      if (!textResult) {
        const errorInfo = parseErrorInfo(lastError);
        const statusCode = errorInfo.isQuota ? 429 : errorInfo.isOverloaded ? 503 : 500;
        return res.status(statusCode).json({
          error: errorInfo.message,
          isQuotaExceeded: errorInfo.isQuota,
          isOverloaded: errorInfo.isOverloaded
        });
      }

      const parsed = JSON.parse(textResult);
      return res.json(parsed);
    } catch (err: unknown) {
      console.error('Błąd OCR ESL:', err);
      const errorInfo = parseErrorInfo(err);
      return res.status(errorInfo.isQuota ? 429 : 500).json({
        error: errorInfo.message,
        isQuotaExceeded: errorInfo.isQuota,
        isOverloaded: errorInfo.isOverloaded
      });
    }
  });

  // Serve static files in production or hook up Vite middlewares in development
  if (process.env.NODE_ENV === 'production') {
    app.use(express.static(path.resolve(__dirname, 'dist')));
    app.get('*', (_req, res) => {
      res.sendFile(path.resolve(__dirname, 'dist', 'index.html'));
    });
  } else {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server listening on http://0.0.0.0:${PORT}`);
  });
}

startServer();
