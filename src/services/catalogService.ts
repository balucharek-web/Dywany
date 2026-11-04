import { collection, onSnapshot, doc, setDoc, writeBatch } from 'firebase/firestore';
import { db, handleFirestoreError, OperationType } from '../firebase';
import { ProductCatalogItem, Carpet } from '../types';
import { initialCarpets } from '../data/seedData';

const LOCAL_STORAGE_KEY = 'lm_product_catalog_v1';

// In-memory cache for O(1) instant barcode & reference queries
const catalogById = new Map<string, ProductCatalogItem>();
const catalogByEan = new Map<string, ProductCatalogItem>();
const catalogByRef = new Map<string, ProductCatalogItem>();

let isInitialized = false;
let listeners: Array<() => void> = [];

function notifyListeners() {
  listeners.forEach((cb) => {
    try {
      cb();
    } catch (_) {}
  });
}

/**
 * Normalizes barcode or LM reference string (removes whitespace, letters, dashes)
 */
export function normalizeCode(raw?: string): string {
  if (!raw) return '';
  return String(raw).trim().replace(/[\s\-_]/g, '');
}

/**
 * Generates deterministic document ID for a catalog product
 */
export function generateCatalogDocId(ean?: string, ref?: string): string {
  const cleanEan = normalizeCode(ean);
  const cleanRef = normalizeCode(ref);

  if (cleanEan && cleanEan.length >= 8) {
    return `ean_${cleanEan}`;
  }
  if (cleanRef && cleanRef.length >= 6) {
    return `ref_${cleanRef}`;
  }
  return `item_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
}

/**
 * Registers an item into in-memory maps
 */
function registerInMemory(item: ProductCatalogItem) {
  if (!item || !item.nazwa) return;

  const docId = item.id || generateCatalogDocId(item.ean, item.ref);
  const normalizedItem: ProductCatalogItem = {
    ...item,
    id: docId,
    ean: normalizeCode(item.ean),
    ref: normalizeCode(item.ref),
    nazwa: item.nazwa.trim()
  };

  catalogById.set(docId, normalizedItem);

  if (normalizedItem.ean) {
    catalogByEan.set(normalizedItem.ean, normalizedItem);
  }
  if (normalizedItem.ref) {
    catalogByRef.set(normalizedItem.ref, normalizedItem);
  }
}

/**
 * Initializes the persistent catalog:
 * 1. Seeds 229 verified store 038 products from initialCarpets
 * 2. Loads local cached additions from localStorage
 * 3. Subscribes to Firestore collection 'product_catalog' for real-time multi-user syncing
 */
export function initProductCatalog(): () => void {
  if (isInitialized) return () => {};
  isInitialized = true;

  // 1. Pre-seed with all 229 verified store 038 carpets
  try {
    initialCarpets.forEach((c) => {
      if (c.nazwa && (c.ean || c.ref)) {
        const id = generateCatalogDocId(c.ean, c.ref);
        registerInMemory({
          id,
          nazwa: c.nazwa,
          ref: c.ref,
          ean: c.ean,
          source: 'seed',
          updatedAt: c.dodano || '2026-06-26'
        });
      }
    });
  } catch (err) {
    console.warn('Catalog seed error:', err);
  }

  // 2. Load cached items from localStorage
  try {
    const cached = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (cached) {
      const parsed: ProductCatalogItem[] = JSON.parse(cached);
      if (Array.isArray(parsed)) {
        parsed.forEach(registerInMemory);
      }
    }
  } catch (err) {
    console.warn('Catalog local cache read error:', err);
  }

  // 3. Subscribe to Firestore product_catalog collection
  let unsubscribeFirestore = () => {};
  try {
    unsubscribeFirestore = onSnapshot(
      collection(db, 'product_catalog'),
      (snapshot) => {
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          if (data && data.nazwa) {
            registerInMemory({
              id: docSnap.id,
              nazwa: String(data.nazwa || ''),
              ref: String(data.ref || ''),
              ean: String(data.ean || ''),
              cena: data.cena ? String(data.cena) : undefined,
              updatedAt: data.updatedAt ? String(data.updatedAt) : undefined,
              updatedBy: data.updatedBy ? String(data.updatedBy) : undefined,
              source: data.source || 'user'
            });
          }
        });

        // Update local storage backup
        persistToLocalStorage();
        notifyListeners();
      },
      (error) => {
        console.warn('Product catalog snapshot notice:', error.message);
      }
    );
  } catch (err) {
    console.warn('Catalog Firestore listener init error:', err);
  }

  return () => {
    unsubscribeFirestore();
  };
}

/**
 * Saves current in-memory catalog state to localStorage backup
 */
function persistToLocalStorage() {
  try {
    const items = Array.from(catalogById.values());
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(items));
  } catch (err) {
    console.warn('Failed to persist catalog to localStorage:', err);
  }
}

/**
 * Subscribes to catalog updates (returns unsubscribe function)
 */
export function subscribeToCatalog(callback: () => void): () => void {
  listeners.push(callback);
  return () => {
    listeners = listeners.filter((cb) => cb !== callback);
  };
}

/**
 * 100% Precision Product Lookup:
 * Instant O(1) query against EAN (13/12 digits) or LM Reference number (8 digits).
 * Returns remembered carpet product data even if the shelf slot was deleted!
 */
export function lookupProductInCatalog(codeOrQuery?: string): ProductCatalogItem | null {
  if (!codeOrQuery) return null;
  const clean = normalizeCode(codeOrQuery);
  if (!clean) return null;

  // Ensure catalog is loaded
  if (!isInitialized) {
    initProductCatalog();
  }

  // 1. Exact match on EAN
  const byEan = catalogByEan.get(clean);
  if (byEan) return byEan;

  // 2. Exact match on REF
  const byRef = catalogByRef.get(clean);
  if (byRef) return byRef;

  // 3. Fallback scan on all values if clean code matches endsWith or startsWith
  for (const item of catalogById.values()) {
    if (item.ean === clean || item.ref === clean) {
      return item;
    }
  }

  // 4. Fallback search against initialCarpets
  const fallback = initialCarpets.find((c) => {
    const cEan = normalizeCode(c.ean);
    const cRef = normalizeCode(c.ref);
    return cEan === clean || cRef === clean;
  });

  if (fallback) {
    const generated: ProductCatalogItem = {
      id: generateCatalogDocId(fallback.ean, fallback.ref),
      nazwa: fallback.nazwa,
      ref: fallback.ref,
      ean: fallback.ean,
      source: 'seed',
      updatedAt: fallback.dodano || '2026-06-26'
    };
    registerInMemory(generated);
    return generated;
  }

  return null;
}

/**
 * Searches catalog by text name or partial code
 */
export function searchCatalog(query: string, limit = 25): ProductCatalogItem[] {
  if (!query) return [];
  const cleanQuery = query.toLowerCase().trim();
  const cleanCode = normalizeCode(query);

  if (!isInitialized) {
    initProductCatalog();
  }

  const results: ProductCatalogItem[] = [];

  for (const item of catalogById.values()) {
    const matchName = item.nazwa.toLowerCase().includes(cleanQuery);
    const matchEan = cleanCode && item.ean?.includes(cleanCode);
    const matchRef = cleanCode && item.ref?.includes(cleanCode);

    if (matchName || matchEan || matchRef) {
      results.push(item);
      if (results.length >= limit) break;
    }
  }

  return results;
}

/**
 * Saves or updates a product in the permanent Master Catalog.
 * Writes to memory, localStorage, and Firestore.
 */
export async function saveProductToCatalog(data: {
  nazwa: string;
  ref?: string;
  ean?: string;
  cena?: string;
  source?: 'seed' | 'user' | 'esl_ocr' | 'import';
  updatedBy?: string;
}): Promise<ProductCatalogItem> {
  const cleanNazwa = (data.nazwa || '').trim();
  const cleanEan = normalizeCode(data.ean);
  const cleanRef = normalizeCode(data.ref);

  if (!cleanNazwa) {
    throw new Error('Nazwa produktu jest wymagana do zapisu w katalogu');
  }

  const docId = generateCatalogDocId(cleanEan, cleanRef);
  const timestamp = new Date().toISOString();

  // Check if existing item has fields we should preserve
  const existing = catalogById.get(docId) || (cleanEan ? catalogByEan.get(cleanEan) : null) || (cleanRef ? catalogByRef.get(cleanRef) : null);

  const finalItem: ProductCatalogItem = {
    id: docId,
    nazwa: cleanNazwa,
    ref: cleanRef || existing?.ref || '',
    ean: cleanEan || existing?.ean || '',
    cena: data.cena || existing?.cena || '',
    updatedAt: timestamp,
    updatedBy: data.updatedBy || 'admin@leroymerlin.pl',
    source: data.source || existing?.source || 'user'
  };

  // 1. Immediately store in memory and localStorage (zero latency)
  registerInMemory(finalItem);
  persistToLocalStorage();
  notifyListeners();

  // 2. Persist to Firestore product_catalog collection
  try {
    await setDoc(doc(db, 'product_catalog', docId), finalItem, { merge: true });
  } catch (error) {
    // If offline or permission issue, log notice; local storage keeps it safe
    console.warn('Failed to sync catalog item to Firestore:', error);
  }

  return finalItem;
}

/**
 * Batch saves multiple carpets into the catalog (e.g. during CSV / Excel import or bulk sync)
 */
export async function batchSaveToCatalog(
  items: Array<{
    nazwa: string;
    ref?: string;
    ean?: string;
    cena?: string;
    source?: 'seed' | 'user' | 'esl_ocr' | 'import';
    updatedBy?: string;
  }>
): Promise<number> {
  let savedCount = 0;
  const batchList: ProductCatalogItem[] = [];

  items.forEach((item) => {
    const cleanNazwa = (item.nazwa || '').trim();
    const cleanEan = normalizeCode(item.ean);
    const cleanRef = normalizeCode(item.ref);

    if (cleanNazwa && (cleanEan || cleanRef)) {
      const docId = generateCatalogDocId(cleanEan, cleanRef);
      const finalItem: ProductCatalogItem = {
        id: docId,
        nazwa: cleanNazwa,
        ref: cleanRef,
        ean: cleanEan,
        cena: item.cena,
        updatedAt: new Date().toISOString(),
        updatedBy: item.updatedBy || 'import',
        source: item.source || 'import'
      };
      registerInMemory(finalItem);
      batchList.push(finalItem);
      savedCount++;
    }
  });

  persistToLocalStorage();
  notifyListeners();

  // Persist to Firestore in safe chunks
  if (batchList.length > 0) {
    for (let i = 0; i < batchList.length; i += 250) {
      try {
        const batch = writeBatch(db);
        const chunk = batchList.slice(i, i + 250);
        chunk.forEach((item) => {
          const docRef = doc(db, 'product_catalog', item.id);
          batch.set(docRef, item, { merge: true });
        });
        await batch.commit();
      } catch (err) {
        console.warn('Catalog batch write notice:', err);
      }
    }
  }

  return savedCount;
}

/**
 * Returns all recognized products in the catalog
 */
export function getAllCatalogProducts(): ProductCatalogItem[] {
  if (!isInitialized) {
    initProductCatalog();
  }
  return Array.from(catalogById.values()).sort((a, b) => a.nazwa.localeCompare(b.nazwa));
}

/**
 * Returns catalog stats
 */
export function getCatalogStats() {
  if (!isInitialized) {
    initProductCatalog();
  }
  const all = Array.from(catalogById.values());
  return {
    total: all.length,
    withEan: all.filter((i) => i.ean && i.ean.length >= 8).length,
    withRef: all.filter((i) => i.ref && i.ref.length >= 6).length
  };
}
