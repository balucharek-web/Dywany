"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getLeroyMerlinSearchUrl = getLeroyMerlinSearchUrl;
exports.fetchProductFromLM = fetchProductFromLM;
const axios_1 = __importDefault(require("axios"));

function getLeroyMerlinSearchUrl(identifier) {
    const cleanId = identifier.trim();
    return `https://www.leroymerlin.pl/szukaj?q=${encodeURIComponent(cleanId)}`;
}

async function fetchProductFromLM(identifier, isEan) {
    const cleanId = identifier.trim();
    if (!cleanId) return null;

    const officialUrl = getLeroyMerlinSearchUrl(cleanId);
    const lmApiKey = process.env.LM_API_KEY;
    const lmApiEndpoint = process.env.LM_API_ENDPOINT;

    if (lmApiKey && lmApiEndpoint) {
        try {
            const response = await axios_1.default.get(`${lmApiEndpoint}/products`, {
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
        } catch (apiErr) {
            console.warn("Leroy Merlin Partner API request failed:", apiErr.message);
        }
    }

    if (isEan && cleanId.length >= 8) {
        try {
            const openProductUrl = `https://world.openproductsfacts.org/api/v0/product/${cleanId}.json`;
            const resp = await axios_1.default.get(openProductUrl, {
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
        } catch (e) {
            // Ignore open registry network failures
        }
    }

    return {
        name: isEan ? `Dywan (EAN: ${cleanId})` : `Dywan (LM: ${cleanId})`,
        ean: isEan ? cleanId : "",
        lmSystemNumber: !isEan ? cleanId : "",
        onlinePrice: 0,
        imageUrl: "",
        productUrl: officialUrl
    };
}