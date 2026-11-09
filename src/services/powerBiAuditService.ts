import { Carpet, PowerBiStockExpoItem } from '../types';
import * as XLSX from 'xlsx';

/**
 * Normalizes 8-digit Leroy Merlin product reference
 */
export function normalizeRefLM(raw: unknown): string {
  if (!raw) return '';
  const str = String(raw).trim().replace(/\s+/g, '');
  // Extract 8-digit sequence if embedded
  const match = str.match(/\b([4-9]\d{7})\b/);
  if (match) return match[1];
  return str;
}

/**
 * Parses pasted text (from Power BI table copy - TSV, CSV, space-delimited or mouse-selected lines)
 */
export function parsePastedPowerBiText(text: string): PowerBiStockExpoItem[] {
  if (!text || !text.trim()) return [];

  const lines = text.trim().split(/\r?\n/);
  if (lines.length === 0) return [];

  const results: PowerBiStockExpoItem[] = [];
  const seenRefs = new Set<string>();

  // Determine delimiter: tab (\t), semicolon (;), comma (,)
  const firstLine = lines[0];
  const delim = firstLine.includes('\t') ? '\t' : firstLine.includes(';') ? ';' : firstLine.includes(',') ? ',' : null;

  // Check if first line is a header
  let refColIdx = -1;
  let opisColIdx = -1;
  let stockColIdx = -1;
  let sklepColIdx = -1;

  if (delim) {
    const headerParts = firstLine.split(delim).map((h) => h.trim().toLowerCase());
    refColIdx = headerParts.findIndex((h) => h.includes('ref') || h.includes('referencj'));
    opisColIdx = headerParts.findIndex((h) => h.includes('opis') || h.includes('nazwa') || h.includes('produkt'));
    stockColIdx = headerParts.findIndex((h) => h.includes('stock') || h.includes('stan') || h.includes('ilo'));
    sklepColIdx = headerParts.findIndex((h) => h.includes('sklep'));
  }

  let startIndex = 0;
  if (refColIdx !== -1) {
    startIndex = 1;
  } else if (delim) {
    // Default Power BI layout if no header or direct copy:
    // sklep (0), ref LM (1), stock (2), dział (3), poddział (4), typ (5), podtyp (6), opis (7)
    sklepColIdx = 0;
    refColIdx = 1;
    stockColIdx = 2;
    opisColIdx = 7;
    startIndex = 0;
  }

  for (let i = startIndex; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line) continue;

    let cleanRef = '';
    let rawOpis = '';
    let parsedStock = 1.0;
    let rawSklep = '';

    // Strategy A: Delimited split if delimiter exists
    if (delim) {
      const parts = line.split(delim).map((p) => p.trim());
      if (parts.length >= 2) {
        let rawRef = parts[refColIdx] || '';
        rawOpis = parts[opisColIdx] || '';
        const rawStock = parts[stockColIdx] || '1';
        rawSklep = parts[sklepColIdx] || '';

        // If ref column doesn't match an 8-digit number, look across parts
        if (!/^\d{7,8}$/.test(rawRef.replace(/\s+/g, ''))) {
          const foundIdx = parts.findIndex((p) => /^\d{8}$/.test(p.replace(/\s+/g, '')));
          if (foundIdx !== -1) {
            rawRef = parts[foundIdx];
          }
        }

        cleanRef = normalizeRefLM(rawRef);
        parsedStock = parseFloat(String(rawStock).replace(',', '.')) || 1.0;

        if (!rawOpis) {
          const longest = parts.reduce((a, b) => (b.length > a.length && !/^\d+$/.test(b) ? b : a), '');
          rawOpis = longest;
        }
      }
    }

    // Strategy B: Fallback regex extraction for mouse-copied / space-separated / unstructured text
    if (!cleanRef) {
      // Find 8-digit Leroy Merlin ref (starts with 4-9 typically, e.g. 74820198, 85248017, 96821517)
      const refMatch = line.match(/\b([4-9]\d{7})\b/) || line.match(/\b(\d{8})\b/);
      if (refMatch) {
        cleanRef = refMatch[1];

        // Try extracting stock (e.g. 1,00 or 1.00)
        const stockMatch = line.match(/\b(\d+[,.]\d{2})\b/);
        if (stockMatch) {
          parsedStock = parseFloat(stockMatch[1].replace(',', '.')) || 1.0;
        }

        // Try extracting store number (e.g. 038 or 54)
        const storeMatch = line.match(/\b(0\d{2}|\d{2,3})\b/);
        if (storeMatch && storeMatch[1] !== cleanRef) {
          rawSklep = storeMatch[1];
        }

        // Extract description by removing metadata tokens
        const cleanedLine = line
          .replace(cleanRef, '')
          .replace(/\b(038|054|54|38)\b/g, '')
          .replace(/\b\d+[,.]\d{2}\b/g, '') // remove 1,00
          .replace(/\b(12|05|5|1|2)\b/g, '') // remove dział / poddział / typ
          .trim();

        rawOpis = cleanedLine.replace(/\s{2,}/g, ' ').trim();
      }
    }

    if (!cleanRef) continue;
    if (seenRefs.has(cleanRef)) continue;
    seenRefs.add(cleanRef);

    if (!rawOpis) {
      rawOpis = `Dywan REF ${cleanRef}`;
    }

    results.push({
      id: cleanRef,
      ref: cleanRef,
      opis: rawOpis,
      stock: parsedStock,
      sklep: rawSklep,
      dzial: '12',
      poddzial: '5'
    });
  }

  return results;
}

/**
 * Parses XLSX / CSV file buffer exported from Power BI
 */
export function parsePowerBiFile(arrayBuffer: ArrayBuffer): PowerBiStockExpoItem[] {
  const data = new Uint8Array(arrayBuffer);
  const workbook = XLSX.read(data, { type: 'array' });
  const firstSheetName = workbook.SheetNames[0];
  const sheet = workbook.Sheets[firstSheetName];
  const rows: any[] = XLSX.utils.sheet_to_json(sheet);

  if (!rows || rows.length === 0) return [];

  const results: PowerBiStockExpoItem[] = [];

  rows.forEach((row, idx) => {
    // Check known Power BI column names
    const rawRef =
      row['ref LM'] ??
      row['ref'] ??
      row['Ref LM'] ??
      row['Ref'] ??
      row['REF'] ??
      row['nr referencji'] ??
      row['Nr referencji'] ??
      row['Reference'] ??
      '';

    const cleanRef = normalizeRefLM(rawRef);
    if (!cleanRef) return;

    const rawOpis =
      row['opis'] ??
      row['Opis'] ??
      row['nazwa'] ??
      row['Nazwa'] ??
      row['Produkt'] ??
      `Dywan ${cleanRef}`;

    const rawStock = row['stock'] ?? row['Stock'] ?? row['stan'] ?? row['Stan'] ?? 1.0;
    const parsedStock = typeof rawStock === 'number' ? rawStock : parseFloat(String(rawStock).replace(',', '.')) || 1.0;

    const rawSklep = String(row['sklep'] ?? row['Sklep'] ?? '').trim();
    const rawDzial = String(row['dział'] ?? row['Dział'] ?? row['dzial'] ?? '12').trim();
    const rawPoddzial = String(row['poddział'] ?? row['Poddział'] ?? row['poddzial'] ?? '5').trim();

    results.push({
      id: cleanRef,
      ref: cleanRef,
      opis: String(rawOpis).trim(),
      stock: parsedStock,
      sklep: rawSklep,
      dzial: rawDzial,
      poddzial: rawPoddzial
    });
  });

  return results;
}

export interface PowerBiComparisonResult {
  pbiItems: PowerBiStockExpoItem[];
  matched: Array<{
    pbi: PowerBiStockExpoItem;
    carpet: Carpet;
  }>;
  missingOnShelf: PowerBiStockExpoItem[];
  excessOnShelf: Carpet[];
  totalPbi: number;
  totalCarpets: number;
  complianceRate: number; // 0 - 100%
}

/**
 * Compares Power BI items against actual carpets physically mapped in store
 */
export function comparePowerBiWithCarpets(
  pbiItems: PowerBiStockExpoItem[],
  carpets: Carpet[]
): PowerBiComparisonResult {
  const carpetsByRef = new Map<string, Carpet>();
  carpets.forEach((c) => {
    if (c.ref) {
      const clean = normalizeRefLM(c.ref);
      if (clean) {
        carpetsByRef.set(clean, c);
      }
    }
  });

  const pbiByRef = new Map<string, PowerBiStockExpoItem>();
  pbiItems.forEach((item) => {
    const clean = normalizeRefLM(item.ref);
    if (clean) {
      pbiByRef.set(clean, item);
    }
  });

  const matched: Array<{ pbi: PowerBiStockExpoItem; carpet: Carpet }> = [];
  const missingOnShelf: PowerBiStockExpoItem[] = [];

  pbiItems.forEach((item) => {
    const clean = normalizeRefLM(item.ref);
    const existing = carpetsByRef.get(clean);
    if (existing) {
      matched.push({ pbi: item, carpet: existing });
    } else {
      missingOnShelf.push(item);
    }
  });

  // Excess carpets: carpets hanging on rack whose REF is NOT in Power BI Stock Expo
  const excessOnShelf: Carpet[] = [];
  carpets.forEach((c) => {
    if (!c.ref) {
      excessOnShelf.push(c);
    } else {
      const clean = normalizeRefLM(c.ref);
      if (!pbiByRef.has(clean)) {
        excessOnShelf.push(c);
      }
    }
  });

  const totalPbi = pbiItems.length;
  const complianceRate = totalPbi > 0 ? Math.round((matched.length / totalPbi) * 100) : 0;

  return {
    pbiItems,
    matched,
    missingOnShelf,
    excessOnShelf,
    totalPbi,
    totalCarpets: carpets.length,
    complianceRate
  };
}
