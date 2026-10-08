/**
 * LeroyMerlinProductService dla Firebase Cloud Functions (Node.js)
 * Odpowiada za bezpieczne, serwerowe pobieranie i parsowanie danych produktów
 * z katalogu Leroy Merlin bez ryzyka CORS i blokad przeglądarkowych.
 */

const https = require("https");
const http = require("http");

const USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

/**
 * Pobiera stronę HTML z Leroy Merlin
 */
function fetchHtml(url) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url);
    const client = parsedUrl.protocol === "https:" ? https : http;

    const options = {
      headers: {
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7"
      },
      timeout: 10000
    };

    client.get(url, options, (res) => {
      // Obsługa przekierowań (301, 302, 307)
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        let redirectUrl = res.headers.location;
        if (!redirectUrl.startsWith("http")) {
          redirectUrl = new URL(redirectUrl, url).toString();
        }
        return resolve(fetchHtml(redirectUrl));
      }

      if (res.statusCode !== 200) {
        return resolve({ statusCode: res.statusCode, html: "", url });
      }

      let data = "";
      res.on("data", (chunk) => { data += chunk; });
      res.on("end", () => resolve({ statusCode: 200, html: data, url }));
    }).on("error", (err) => reject(err));
  });
}

/**
 * Parsowanie strukturalnych danych Schema.org JSON-LD oraz meta tagów
 */
function parseProductHtml(html, sourceUrl, query, expectedKm, expectedEan) {
  let foundName = "";
  let foundPrice = null;
  let foundCurrency = "PLN";
  let foundKm = expectedKm || "";
  let foundEan = expectedEan || "";
  let foundUrl = sourceUrl;

  // 1. Schema.org JSON-LD
  const jsonLdRegex = /<script[^>]+type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi;
  let match;

  while ((match = jsonLdRegex.exec(html)) !== null) {
    try {
      const content = match[1].trim();
      const parsed = JSON.parse(content);
      const items = Array.isArray(parsed) ? parsed : [parsed];

      for (const item of items) {
        if (item["@type"] && item["@type"].toLowerCase() === "product") {
          if (item.name) foundName = item.name;
          if (item.sku && /^[0-9]{8}$/.test(item.sku)) foundKm = item.sku;
          if (item.gtin13 || item.gtin) foundEan = item.gtin13 || item.gtin;
          if (item.url) foundUrl = item.url;

          if (item.offers) {
            const offer = Array.isArray(item.offers) ? item.offers[0] : item.offers;
            if (offer && offer.price) {
              const p = parseFloat(offer.price);
              if (!isNaN(p)) foundPrice = p;
            }
            if (offer && offer.priceCurrency) foundCurrency = offer.priceCurrency;
          }
          break;
        }
      }
      if (foundName && foundPrice) break;
    } catch (e) {
      // Ignoruj błędy pojedynczego tagu JSON-LD
    }
  }

  // 2. OpenGraph fallback
  if (!foundName) {
    const ogTitle = /<meta[^>]+(?:property|name)=["']og:title["'][^>]+content=["'](.*?)["']/i.exec(html);
    if (ogTitle && ogTitle[1]) {
      foundName = ogTitle[1].replace(" - Leroy Merlin", "").replace(" w sklepach Leroy Merlin", "").trim();
    }
  }

  if (foundPrice === null) {
    const ogPrice = /<meta[^>]+(?:property|name)=["']product:price:amount["'][^>]+content=["'](.*?)["']/i.exec(html);
    if (ogPrice && ogPrice[1]) {
      const p = parseFloat(ogPrice[1].replace(",", "."));
      if (!isNaN(p)) foundPrice = p;
    }
  }

  // 3. Wymiary dywanu
  const dimRegex = /(\d{2,3}\s*(?:x|×|X)\s*\d{2,3}(?:\s*cm)?)/i;
  const dimMatch = dimRegex.exec(foundName);
  let foundRozmiar = "";
  if (dimMatch && dimMatch[1]) {
    foundRozmiar = dimMatch[1].trim();
    if (!foundRozmiar.toLowerCase().includes("cm")) {
      foundRozmiar += " cm";
    }
  }

  if (foundName || foundPrice !== null) {
    return {
      km: foundKm || (/^[0-9]{8}$/.test(query) ? query : ""),
      ean: foundEan || (/^[0-9]{12,14}$/.test(query) ? query : ""),
      nazwa: foundName,
      rozmiar: foundRozmiar,
      cena: foundPrice,
      waluta: foundCurrency,
      productUrl: foundUrl,
      status: "ACTIVE",
      source: "leroy_merlin"
    };
  }

  return {
    km: expectedKm || (/^[0-9]{8}$/.test(query) ? query : ""),
    ean: expectedEan || (/^[0-9]{12,14}$/.test(query) ? query : ""),
    nazwa: "",
    rozmiar: "",
    cena: null,
    waluta: "PLN",
    productUrl: sourceUrl,
    status: "NOT_FOUND",
    source: "leroy_merlin"
  };
}

/**
 * Główna funkcja serwisowa
 */
async function resolveProduct(identifier) {
  const cleanId = (identifier || "").trim();
  if (!cleanId) {
    return { status: "ERROR", message: "Pusty identyfikator" };
  }

  const isKm = /^[0-9]{8}$/.test(cleanId);
  const isEan = /^[0-9]{12,14}$/.test(cleanId);

  const searchUrl = `https://www.leroymerlin.pl/szukaj?q=${encodeURIComponent(cleanId)}`;

  try {
    const res = await fetchHtml(searchUrl);
    if (res.statusCode === 404) {
      return {
        km: isKm ? cleanId : "",
        ean: isEan ? cleanId : "",
        status: "NOT_FOUND"
      };
    }

    return parseProductHtml(
      res.html,
      res.url,
      cleanId,
      isKm ? cleanId : null,
      isEan ? cleanId : null
    );
  } catch (err) {
    console.error("Błąd podczas pobierania produktu z Leroy Merlin:", err);
    return {
      km: isKm ? cleanId : "",
      ean: isEan ? cleanId : "",
      status: "ERROR",
      message: err.message
    };
  }
}

module.exports = {
  resolveProduct
};
