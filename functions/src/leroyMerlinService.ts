import axios from "axios";
import * as cheerio from "cheerio";

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

const USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

/**
 * Searches and fetches rug details from Leroy Merlin Poland website
 * using structured JSON-LD data and product meta tags.
 */
export async function fetchProductFromLM(identifier: string, isEan: boolean): Promise<LeroyProductData | null> {
  const cleanId = identifier.trim();
  if (!cleanId) return null;

  try {
    // 1. First attempt: Direct search URL on leroymerlin.pl
    const searchUrl = `https://www.leroymerlin.pl/szukaj?q=${encodeURIComponent(cleanId)}`;
    const response = await axios.get(searchUrl, {
      headers: {
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "pl,en-US;q=0.7,en;q=0.3"
      },
      timeout: 10000,
      maxRedirects: 5
    });

    const html = response.data;
    if (typeof html !== "string") return null;

    const $ = cheerio.load(html);

    // Check for JSON-LD structured data (standard on Leroy Merlin e-commerce product pages)
    let productData: Partial<LeroyProductData> = {};

    $('script[type="application/ld+json"]').each((_, element) => {
      try {
        const text = $(element).html();
        if (!text) return;
        const parsed = JSON.parse(text);

        const item = parsed["@type"] === "Product" ? parsed : (Array.isArray(parsed["@graph"]) ? parsed["@graph"].find((i: any) => i["@type"] === "Product") : null);

        if (item) {
          const price = item.offers?.price ? parseFloat(item.offers.price) : 0;
          const eanFound = item.gtin13 || item.gtin || (isEan ? cleanId : "");
          const skuFound = item.sku || (!isEan ? cleanId : "");
          const imgFound = Array.isArray(item.image) ? item.image[0] : (item.image?.url || item.image || "");

          productData = {
            name: item.name || "",
            ean: eanFound || (isEan ? cleanId : ""),
            lmSystemNumber: skuFound || (!isEan ? cleanId : ""),
            onlinePrice: price,
            imageUrl: imgFound,
            productUrl: response.request?.res?.responseUrl || searchUrl
          };
        }
      } catch (err) {
        // Continue parsing next script tag
      }
    });

    if (productData.name && productData.onlinePrice) {
      return {
        name: productData.name,
        ean: productData.ean || (isEan ? cleanId : ""),
        lmSystemNumber: productData.lmSystemNumber || (!isEan ? cleanId : ""),
        onlinePrice: productData.onlinePrice,
        imageUrl: productData.imageUrl || "",
        productUrl: productData.productUrl || searchUrl
      };
    }

    // Fallback: OpenGraph and standard DOM elements
    const ogTitle = $('meta[property="og:title"]').attr("content") || $("h1").first().text().trim();
    const ogImage = $('meta[property="og:image"]').attr("content") || "";
    const rawPrice = $('[data-test="product-price"], .price, .product-price').first().text().replace(/[^\d.,]/g, "").replace(",", ".");
    const priceVal = parseFloat(rawPrice) || 0;

    if (ogTitle) {
      return {
        name: ogTitle,
        ean: isEan ? cleanId : "",
        lmSystemNumber: !isEan ? cleanId : "",
        onlinePrice: priceVal,
        imageUrl: ogImage,
        productUrl: response.request?.res?.responseUrl || searchUrl
      };
    }

    return null;
  } catch (error: any) {
    console.error(`Błąd pobierania danych produktu z Leroy Merlin (${cleanId}):`, error.message);
    return null;
  }
}
