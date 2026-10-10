import axios from "axios";

export interface LeroyProductData {
  name: string;
  ean: string;
  lmSystemNumber: string;
  onlinePrice: number;
  imageUrl: string;
  productUrl: string;
  dimensions?: string;
  composition?: string;
}

/**
 * Generates the official Leroy Merlin Poland search URL for a given identifier
 * (EAN barcode or LM system number).
 * 
 * Experienced Retail System Note:
 * Modern e-commerce sites like leroymerlin.pl are protected by Varnish/Cloudflare/Akamai bot mitigations.
 * Automated server-side HTML scraping is deliberately blocked (HTTP 403/405).
 * Legitimate, robust enterprise solutions:
 * 1. Deep-link the user directly to the official product page in their browser session.
 * 2. Cache verified products in Firestore so any scan by any employee auto-completes for the store.
 * 3. Query open, official barcode registries for generic EAN metadata.
 * 4. Connect to Leroy Merlin Partner API when credentials are provided.
 */
export function getLeroyMerlinSearchUrl(identifier: string): string {
  const cleanId = identifier.trim();
  return `https://www.leroymerlin.pl/szukaj?q=${encodeURIComponent(cleanId)}`;
}

/**
 * Searches and fetches product data without relying on fragile, blocked HTML scraping.
 */
export async function fetchProductFromLM(identifier: string, isEan: boolean): Promise<LeroyProductData | null> {
  const cleanId = identifier.trim();
  if (!cleanId) return null;

  const officialUrl = getLeroyMerlinSearchUrl(cleanId);

  // 1. Check if an official Leroy Merlin Enterprise/Partner API is configured
  const lmApiKey = process.env.LM_API_KEY;
  const lmApiEndpoint = process.env.LM_API_ENDPOINT;

  if (lmApiKey && lmApiEndpoint) {
    try {
      const response = await axios.get(`${lmApiEndpoint}/products`, {
        params: isEan ? { ean: cleanId } : { sku: cleanId },
        headers: {
          "Authorization": `Bearer ${lmApiKey}`,
          "Accept": "application/json"
        },
        timeout: 5000
      });

      if (response.data && response.data.product) {
        const item = response.data.product;
        return {
          name: item.name || `Dywan ${cleanId}`,
          ean: item.ean || (isEan ? cleanId : ""),
          lmSystemNumber: item.sku || (!isEan ? cleanId : ""),
          onlinePrice: Number(item.price) || 0,
          imageUrl: item.imageUrl || "",
          productUrl: item.url || officialUrl,
          dimensions: item.dimensions,
          composition: item.composition
        };
      }
    } catch (apiErr: any) {
      console.warn("Leroy Merlin Partner API request failed:", apiErr.message);
    }
  }

  // 2. If EAN is provided, query Open Product Facts API (Open and legal REST API without bot blockers)
  if (isEan && cleanId.length >= 8) {
    try {
      const openProductUrl = `https://world.openproductsfacts.org/api/v0/product/${cleanId}.json`;
      const resp = await axios.get(openProductUrl, {
        headers: { "User-Agent": "LeroyMerlinRugDisplayApp/1.0" },
        timeout: 4000
      });

      if (resp.data && resp.data.status === 1 && resp.data.product) {
        const p = resp.data.product;
        return {
          name: p.product_name_pl || p.product_name || `Dywan EAN ${cleanId}`,
          ean: cleanId,
          lmSystemNumber: "",
          onlinePrice: 0,
          imageUrl: p.image_url || p.image_front_url || "",
          productUrl: officialUrl,
          dimensions: "",
          composition: ""
        };
      }
    } catch (e: any) {
      // Ignore open registry network failures
    }
  }

  // 3. Fallback: Return structured reference object with official link
  return {
    name: isEan ? `Dywan (EAN: ${cleanId})` : `Dywan (LM: ${cleanId})`,
    ean: isEan ? cleanId : "",
    lmSystemNumber: !isEan ? cleanId : "",
    onlinePrice: 0,
    imageUrl: "",
    productUrl: officialUrl
  };
}
