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

  // In-memory cache for ultra-fast lookup (sub-millisecond)
  const specsCache = new Map<string, any>();

  // Pre-indexed Leroy Merlin carpet specifications (exact match by REF)
  const KNOWN_LM_SPECS_BY_REF: Record<string, any> = {
    // Dywan Lagoon Morski / Balta (as shown in user's Leroy Merlin screenshot)
    '92116369': {
      grubosc: '5.5 mm',
      materialGlowny: 'Polipropylen',
      sklad: '96% PP + 4% Lateks',
      tekstura: 'Flat weave',
      wysokoscRuna: 'Płaskie włosie',
      miejsceUzytkowania: 'Wewnętrzny / zewnętrzny',
      marka: 'INSPIRE',
      model: 'LAGOON',
      spodAntyposlizgowy: 'Nie',
      wlasciwosci: 'Do wnętrz i na zewnątrz (odporny na warunki atmosferyczne i UV), łatwy w czyszczeniu',
      przeznaczenie: 'Taras / Balkon / Przedpokój / Salon',
      ksztalt: 'Prostokątny'
    },
    '92116398': {
      grubosc: '5.5 mm',
      materialGlowny: 'Polipropylen',
      sklad: '96% PP + 4% Lateks',
      tekstura: 'Flat weave',
      wysokoscRuna: 'Płaskie włosie',
      miejsceUzytkowania: 'Wewnętrzny / zewnętrzny',
      marka: 'INSPIRE',
      model: 'LAGOON',
      spodAntyposlizgowy: 'Nie',
      wlasciwosci: 'Do wnętrz i na zewnątrz, odporny na wilgoć i słońce',
      przeznaczenie: 'Taras / Balkon / Salon',
      ksztalt: 'Prostokątny'
    },
    '87797153': {
      grubosc: '11 mm',
      materialGlowny: 'Polipropylen',
      sklad: '100% Polipropylen Heat-set',
      tekstura: 'Frise',
      wysokoscRuna: '9 mm',
      miejsceUzytkowania: 'Wewnętrzny',
      marka: 'INSPIRE',
      model: 'BLIDA',
      spodAntyposlizgowy: 'Nie',
      wlasciwosci: 'Gęste, sprężyste runo, wysoka odporność na udeptywanie',
      przeznaczenie: 'Salon / Przedpokój',
      ksztalt: 'Prostokątny'
    },
    '82850488': {
      grubosc: '14 mm',
      materialGlowny: 'Poliester',
      sklad: '100% Poliester Microfibre',
      tekstura: 'Soft touch',
      wysokoscRuna: '12 mm',
      miejsceUzytkowania: 'Wewnętrzny',
      marka: 'INSPIRE',
      model: 'AMBA',
      spodAntyposlizgowy: 'Tak',
      wlasciwosci: 'Aksamitnie miękki w dotyku, można prać w pralce',
      przeznaczenie: 'Sypialnia / Pokój dzienny',
      ksztalt: 'Prostokątny'
    },
    '96821522': {
      grubosc: '9.5 mm',
      materialGlowny: 'Polipropylen',
      sklad: '100% Polipropylen',
      tekstura: 'Cut pile',
      wysokoscRuna: '8 mm',
      miejsceUzytkowania: 'Wewnętrzny',
      marka: 'BALTA',
      model: 'SPARKLES',
      spodAntyposlizgowy: 'Nie',
      wlasciwosci: 'Błyszczące włókna, puszyste runo, ogrzewanie podłogowe',
      przeznaczenie: 'Salon / Sypialnia',
      ksztalt: 'Prostokątny'
    },
    '91659479': {
      grubosc: '7 mm',
      materialGlowny: 'Polipropylen',
      sklad: '100% Polipropylen',
      tekstura: 'Pętelkowy / Strukturalny',
      wysokoscRuna: '5.5 mm',
      miejsceUzytkowania: 'Wewnętrzny',
      marka: 'INSPIRE',
      model: 'PACYFIK',
      spodAntyposlizgowy: 'Filc',
      wlasciwosci: 'Wysoka odporność na intensywne użytkowanie, krzesła biurowe',
      przeznaczenie: 'Przedpokój / Biuro / Korytarz',
      ksztalt: 'Prostokątny'
    }
  };

  // Fast pattern matcher for Leroy Merlin rug collections
  function matchKnownLmCollection(nazwa: string = ''): any | null {
    const n = nazwa.toLowerCase();
    if (n.includes('lagoon') || n.includes('flat weave') || n.includes('płasko') || n.includes('zewn') || n.includes('taras')) {
      return {
        grubosc: '5.5 mm',
        materialGlowny: 'Polipropylen',
        sklad: '96% PP + 4% Lateks',
        tekstura: 'Flat weave',
        wysokoscRuna: 'Płaskie włosie',
        miejsceUzytkowania: 'Wewnętrzny / zewnętrzny',
        marka: 'INSPIRE',
        model: 'LAGOON',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Do wnętrz i na zewnątrz (odporny na warunki atmosferyczne i UV), łatwy w czyszczeniu',
        przeznaczenie: 'Taras / Balkon / Przedpokój / Salon',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('sparkles') || n.includes('sparkle')) {
      return {
        grubosc: '9.5 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen',
        tekstura: 'Cut pile',
        wysokoscRuna: '8 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'BALTA',
        model: 'SPARKLES',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Błyszczące włókna, puszyste runo, ogrzewanie podłogowe',
        przeznaczenie: 'Salon / Sypialnia',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('blida')) {
      return {
        grubosc: '11 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen Heat-set',
        tekstura: 'Frise',
        wysokoscRuna: '9 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'BLIDA',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Gęste, sprężyste runo, wysoka odporność na udeptywanie',
        przeznaczenie: 'Salon / Przedpokój',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('amba')) {
      return {
        grubosc: '14 mm',
        materialGlowny: 'Poliester',
        sklad: '100% Poliester Microfibre',
        tekstura: 'Soft touch',
        wysokoscRuna: '12 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'AMBA',
        spodAntyposlizgowy: 'Tak',
        wlasciwosci: 'Aksamitnie miękki w dotyku, można prać w pralce',
        przeznaczenie: 'Sypialnia / Pokój dzienny',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('pacyfik')) {
      return {
        grubosc: '7 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen',
        tekstura: 'Pętelkowy / Strukturalny',
        wysokoscRuna: '5.5 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'PACYFIK',
        spodAntyposlizgowy: 'Filc',
        wlasciwosci: 'Wysoka odporność na intensywne użytkowanie, krzesła biurowe',
        przeznaczenie: 'Przedpokój / Biuro / Korytarz',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('shaggy') || n.includes('włochacz')) {
      return {
        grubosc: '33 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen Shaggy',
        tekstura: 'Shaggy długowłose',
        wysokoscRuna: '30 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'SHAGGY DELUXE',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Bardzo puszysty, doskonała izolacja termiczna, miękkie runo',
        przeznaczenie: 'Salon / Sypialnia',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('juta') || n.includes('jute') || n.includes('boho')) {
      return {
        grubosc: '6 mm',
        materialGlowny: 'Juta',
        sklad: '100% Juta naturalna',
        tekstura: 'Plecionka ręczna / maszynowa',
        wysokoscRuna: 'Płaski splot',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'BOHO JUTE',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Naturalny, ekologiczny, surowiec odnawialny',
        przeznaczenie: 'Salon / Jadalnia',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('rabbit') || n.includes('królik') || n.includes('futro')) {
      return {
        grubosc: '22 mm',
        materialGlowny: 'Poliester',
        sklad: '100% Poliester Rabbit Fur',
        tekstura: 'Futerkowe / Ultra soft',
        wysokoscRuna: '20 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'RABBIT',
        spodAntyposlizgowy: 'Tak',
        wlasciwosci: 'Efekt naturalnego futra, ultra miękki, spód antypoślizgowy',
        przeznaczenie: 'Sypialnia / Pokój dziecięcy',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('jimma')) {
      return {
        grubosc: '6 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen',
        tekstura: 'Flat weave (tkany na płasko)',
        wysokoscRuna: 'Płaskie włosie',
        miejsceUzytkowania: 'Wewnętrzny / zewnętrzny',
        marka: 'INSPIRE',
        model: 'JIMMA',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Do wnętrz i na zewnątrz (odporny na wilgoć i promienie UV), łatwy w myciu',
        przeznaczenie: 'Taras / Balkon / Przedpokój / Kuchnia',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('nils')) {
      return {
        grubosc: '6 mm',
        materialGlowny: 'Juta',
        sklad: '100% Juta naturalna',
        tekstura: 'Plecionka ręczna',
        wysokoscRuna: 'Płaski splot',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'NILS JUTE',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Naturalny surowiec, biodegradowalny, ekologiczny styl boho',
        przeznaczenie: 'Salon / Sypialnia / Jadalnia',
        ksztalt: n.includes('owal') ? 'Owalny' : 'Prostokątny'
      };
    }
    if (n.includes('kashmir')) {
      return {
        grubosc: '13 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen Heat-set Soft',
        tekstura: 'Cut pile miękkie',
        wysokoscRuna: '11 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'KASHMIR',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Aksamitny w dotyku, efekt dwupoziomowy 3D, ogrzewanie podłogowe',
        przeznaczenie: 'Salon / Sypialnia',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('kalabria') || n.includes('bora')) {
      return {
        grubosc: '10 mm',
        materialGlowny: 'Polipropylen',
        sklad: '100% Polipropylen BCF',
        tekstura: 'Gęsty splot klasyczny',
        wysokoscRuna: '8 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'BORA / KALABRIA',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Klasyczny wzór medalion, wysoka trwałość kolorów, łatwe odkurzanie',
        przeznaczenie: 'Salon / Pokój dzienny',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('jesper')) {
      return {
        grubosc: '8 mm',
        materialGlowny: 'Bawełna / Poliester',
        sklad: '80% Bawełna + 20% Poliester',
        tekstura: 'Płaski splot z frędzlami',
        wysokoscRuna: 'Płaskie włosie',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'INSPIRE',
        model: 'JESPER',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Styl skandynawski, naturalna bawełna, frędzle',
        przeznaczenie: 'Salon / Sypialnia / Gabinet',
        ksztalt: 'Prostokątny'
      };
    }
    if (n.includes('wełn') || n.includes('wool') || n.includes('agnella')) {
      return {
        grubosc: '10 mm',
        materialGlowny: 'Wełna',
        sklad: '100% Wełna',
        tekstura: 'Gęste runo cięte',
        wysokoscRuna: '8.5 mm',
        miejsceUzytkowania: 'Wewnętrzny',
        marka: 'AGNELLA',
        model: 'NATURAL WOOL',
        spodAntyposlizgowy: 'Nie',
        wlasciwosci: 'Naturalna wełna, trudnopalny, doskonała izolacja akustyczna',
        przeznaczenie: 'Salon / Gabinet',
        ksztalt: 'Prostokątny'
      };
    }
    return null;
  }

  // Carpet specifications endpoint (fetch exact Leroy Merlin product specs)
  app.post('/api/carpet-specs', async (req, res) => {
    try {
      const { ref, ean, nazwa } = req.body;
      if (!ref && !ean && !nazwa) {
        return res.status(400).json({ error: 'Brak danych dywanu do wyszukania' });
      }

      // 1. Ultra-fast in-memory cache check
      const cacheKey = `${ref || ''}_${ean || ''}_${nazwa || ''}`.trim();
      if (specsCache.has(cacheKey)) {
        return res.json(specsCache.get(cacheKey));
      }

      // 2. Ultra-fast exact REF lookup
      const cleanRef = (ref || '').replace(/\D/g, '');
      if (cleanRef && KNOWN_LM_SPECS_BY_REF[cleanRef]) {
        const result = { ...KNOWN_LM_SPECS_BY_REF[cleanRef], source: 'catalog_exact' };
        specsCache.set(cacheKey, result);
        return res.json(result);
      }

      // 3. Fast collection pattern match
      const collectionMatch = matchKnownLmCollection(nazwa);
      if (collectionMatch) {
        const result = { ...collectionMatch, source: 'catalog_pattern' };
        specsCache.set(cacheKey, result);
        return res.json(result);
      }

      // 4. Quick AI inference with gemini-3.1-flash-lite (fastest low-latency model)
      const promptText = `Jesteś ekspertem ds. bazy produktów sieci Leroy Merlin.
Na podstawie parametrów podaj oficjalną tabelę specyfikacji technicznej dywanu Leroy Merlin:
- Numer REF: "${ref || ''}"
- Nazwa produktu: "${nazwa || ''}"
- Kod EAN: "${ean || ''}"

Wypełnij pola dokładnie tak jak w karcie technicznej Leroy Merlin:
- "grubosc": Grubość całkowita w mm (np. "5.5 mm", "11 mm", "14 mm", "33 mm").
- "materialGlowny": Główny surowiec (np. "Polipropylen", "Poliester", "Juta", "Wełna").
- "sklad": Skład procentowy runa i podkładu (np. "96% PP + 4% Lateks", "100% Polipropylen", "100% Poliester").
- "tekstura": Rodzaj splotu (np. "Flat weave", "Frise", "Cut pile", "Soft touch", "Shaggy", "Plecionka").
- "wysokoscRuna": Długość runa (np. "Płaskie włosie", "8 mm", "9 mm", "12 mm", "30 mm").
- "miejsceUzytkowania": Miejsce (np. "Wewnętrzny / zewnętrzny" lub "Wewnętrzny").
- "marka": Marka produktu (np. "INSPIRE", "BALTA", "AGNELLA").
- "model": Model lub kolekcja produktu (np. "LAGOON", "BLIDA", "AMBA", "SPARKLES").
- "spodAntyposlizgowy": Czy ma spód antypoślizgowy ("Tak" lub "Nie").
- "wlasciwosci": 2 najważniejsze cechy (np. "Do wnętrz i na zewnątrz (odporny na UV i wilgoć)", "Na ogrzewanie podłogowe, łatwe odkurzanie").
- "przeznaczenie": Pomieszczenia (np. "Taras / Balkon / Salon", "Salon / Sypialnia", "Przedpokój / Biuro").

Zwróć wynik wyłącznie jako JSON.`;

      let aiResult: any = null;
      try {
        // Use flash-lite for sub-second speed
        const aiPromise = ai.models.generateContent({
          model: 'gemini-3.1-flash-lite',
          contents: promptText,
          config: {
            responseMimeType: 'application/json',
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                grubosc: { type: Type.STRING },
                materialGlowny: { type: Type.STRING },
                sklad: { type: Type.STRING },
                tekstura: { type: Type.STRING },
                wysokoscRuna: { type: Type.STRING },
                miejsceUzytkowania: { type: Type.STRING },
                marka: { type: Type.STRING },
                model: { type: Type.STRING },
                spodAntyposlizgowy: { type: Type.STRING },
                wlasciwosci: { type: Type.STRING },
                przeznaczenie: { type: Type.STRING }
              },
              required: ['sklad', 'wysokoscRuna', 'grubosc']
            }
          }
        });

        // 1.8 second strict timeout to guarantee rapid response
        const timeoutPromise = new Promise((_, reject) =>
          setTimeout(() => reject(new Error('AI_TIMEOUT')), 1800)
        );

        const response: any = await Promise.race([aiPromise, timeoutPromise]);
        if (response?.text) {
          aiResult = JSON.parse(response.text);
        }
      } catch (aiErr: any) {
        console.warn('Fast AI lookup warning:', aiErr?.message || aiErr);
      }

      let fallbackObj: any = null;
      if (!aiResult) {
        const nameLower = (nazwa || '').toLowerCase();
        let fGrubosc = '9 mm';
        let fMaterial = 'Polipropylen';
        let fSklad = '100% Polipropylen';
        let fTekstura = 'Cut pile';
        let fWysokosc = '8 mm';
        let fMiejsce = 'Wewnętrzny';
        let fSpod = 'Nie';
        let fMarka = 'INSPIRE';
        let fModel = (nazwa || '').split(' ')[1] || 'STANDARD';
        let fWlasciwosci = 'Ogrzewanie podłogowe, łatwy w odkurzaniu';
        let fPrzeznaczenie = 'Salon / Sypialnia';

        if (nameLower.includes('juta') || nameLower.includes('jute')) {
          fMaterial = 'Juta';
          fSklad = '100% Juta naturalna';
          fTekstura = 'Plecionka ręczna';
          fWysokosc = 'Płaski splot';
          fGrubosc = '6 mm';
          fWlasciwosci = 'Naturalne włókno, ekologiczny, styl boho';
          fPrzeznaczenie = 'Salon / Jadalnia';
        } else if (nameLower.includes('płasko') || nameLower.includes('flat') || nameLower.includes('zewn') || nameLower.includes('taras')) {
          fTekstura = 'Flat weave (tkany na płasko)';
          fWysokosc = 'Płaskie włosie';
          fGrubosc = '5.5 mm';
          fMiejsce = 'Wewnętrzny / zewnętrzny';
          fWlasciwosci = 'Odporny na warunki atmosferyczne i wilgoć, łatwy w myciu';
          fPrzeznaczenie = 'Taras / Balkon / Przedpokój / Kuchnia';
        } else if (nameLower.includes('shaggy') || nameLower.includes('włochacz')) {
          fTekstura = 'Shaggy długowłose';
          fWysokosc = '30 mm';
          fGrubosc = '33 mm';
          fWlasciwosci = 'Puszyste, miękkie runo, dobra izolacja termiczna';
          fPrzeznaczenie = 'Salon / Sypialnia';
        } else if (nameLower.includes('rabbit') || nameLower.includes('królik') || nameLower.includes('fur')) {
          fMaterial = 'Poliester';
          fSklad = '100% Poliester Rabbit Fur';
          fTekstura = 'Ultra soft futerkowe';
          fWysokosc = '20 mm';
          fGrubosc = '22 mm';
          fSpod = 'Tak';
          fPrzeznaczenie = 'Sypialnia / Pokój dziecięcy';
        } else if (nameLower.includes('frise') || nameLower.includes('heat-set') || nameLower.includes('heatset')) {
          fTekstura = 'Frise sprężyste';
          fSklad = '100% Polipropylen Heat-set';
          fWysokosc = '9 mm';
          fGrubosc = '11 mm';
          fWlasciwosci = 'Gęste, odporne na udeptywanie runo';
        } else if (nameLower.includes('wełn') || nameLower.includes('wool')) {
          fMaterial = 'Wełna';
          fSklad = '100% Wełna naturalna';
          fGrubosc = '10 mm';
          fWysokosc = '8.5 mm';
          fMarka = 'AGNELLA';
          fWlasciwosci = 'Trudnopalny, reguluje wilgotność, trwałość na lata';
        }

        fallbackObj = {
          grubosc: fGrubosc,
          materialGlowny: fMaterial,
          sklad: fSklad,
          tekstura: fTekstura,
          wysokoscRuna: fWysokosc,
          miejsceUzytkowania: fMiejsce,
          marka: fMarka,
          model: fModel.toUpperCase(),
          spodAntyposlizgowy: fSpod,
          wlasciwosci: fWlasciwosci,
          przeznaczenie: fPrzeznaczenie,
          isFallback: true
        };
      }

      const finalResult = aiResult || fallbackObj;

      specsCache.set(cacheKey, finalResult);
      return res.json(finalResult);
    } catch (err: unknown) {
      console.error('Błąd pobierania specyfikacji dywanu:', err);
      return res.status(500).json({ error: 'Błąd przetwarzania specyfikacji' });
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
