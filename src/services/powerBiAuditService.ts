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

/**
 * Robust A4 print engine using isolated iframe to guarantee print dialog works across all browsers
 */
export function printMissingRugsDocument(
  items: PowerBiStockExpoItem[],
  storeNumber: string,
  storeCity: string,
  totalPbi: number,
  matchedCount: number,
  complianceRate: number
) {
  const dateStr = new Date().toLocaleDateString('pl-PL');
  const timeStr = new Date().toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });

  const rowsHtml = items
    .map(
      (item, idx) => `
    <tr style="border-bottom: 1px solid #999; page-break-inside: avoid;">
      <td style="padding: 5px; text-align: center; font-weight: bold; background: #f9fafb; font-family: monospace; font-size: 10px; width: 35px; border: 1px solid #000;">${idx + 1}</td>
      <td style="padding: 5px; text-align: center; font-weight: 900; font-family: monospace; font-size: 12px; width: 95px; border: 1px solid #000;">${item.ref}</td>
      <td style="padding: 5px; font-weight: bold; font-size: 11px; line-height: 1.2; border: 1px solid #000;">${item.opis}</td>
      <td style="padding: 5px; text-align: center; font-family: monospace; font-weight: bold; font-size: 11px; width: 55px; border: 1px solid #000;">${item.stock.toFixed(2).replace('.', ',')}</td>
      <td style="padding: 5px; text-align: center; font-size: 10px; color: #4b5563; font-family: monospace; width: 130px; border: 1px solid #000;">Regał: ____ Slot: [A/B]</td>
      <td style="padding: 5px; text-align: center; width: 90px; border: 1px solid #000;"><div style="display: inline-block; width: 14px; height: 14px; border: 2px solid #000; border-radius: 2px;"></div></td>
    </tr>
  `
    )
    .join('');

  const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Braki Stock Expo - Sklep ${storeNumber}</title>
  <style>
    @page { size: A4 portrait; margin: 8mm 6mm; }
    * { box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; color: #000; margin: 0; padding: 0; background: #fff; font-size: 11px; }
    table { width: 100%; border-collapse: collapse; margin-top: 8px; border: 1px solid #000; }
    th { background: #f3f4f6; font-weight: 900; font-size: 10px; text-transform: uppercase; padding: 6px 4px; border: 1px solid #000; }
    .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #000; padding-bottom: 8px; margin-bottom: 8px; }
    .footer { margin-top: 10px; padding-top: 6px; border-top: 1px solid #999; display: flex; justify-content: space-between; font-size: 9px; color: #555; }
    @media print {
      body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    }
  </style>
</head>
<body>
  <div class="header">
    <div>
      <div style="font-size: 15px; font-weight: 900; text-transform: uppercase; letter-spacing: -0.5px;">
        Audyt Stock Expo Power BI — Lista braków do powieszenia
      </div>
      <div style="font-size: 11px; font-weight: bold; color: #374151; margin-top: 2px;">
        LEROY MERLIN • Sklep ${storeNumber} ${storeCity} • Dział 12 Dywany (Poddział 05)
      </div>
    </div>
    <div style="text-align: right; font-size: 10px; line-height: 1.3;">
      <div>Wydruk: <strong>${dateStr} ${timeStr}</strong></div>
      <div>Power BI: <strong>${totalPbi} poz.</strong> | Na regale: <strong>${matchedCount}</strong> (${complianceRate}%)</div>
      <div>Do powieszenia (Braki): <strong style="color: #b91c1c; font-size: 12px;">${items.length}</strong></div>
    </div>
  </div>

  ${
    items.length === 0
      ? `
    <div style="padding: 30px; text-align: center; border: 1px solid #000;">
      <div style="font-size: 14px; font-weight: bold;">Wszystkie dywany z raportu Power BI wiszą na regałach! (100% zgodności)</div>
    </div>
  `
      : `
    <table>
      <thead>
        <tr>
          <th style="width: 35px; text-align: center;">LP</th>
          <th style="width: 95px; text-align: center;">REF LM</th>
          <th style="text-align: left;">Nazwa / Opis dywanu (Power BI)</th>
          <th style="width: 55px; text-align: center;">Stock</th>
          <th style="width: 130px; text-align: center;">Zawieś na regale</th>
          <th style="width: 90px; text-align: center;">Powieszono</th>
        </tr>
      </thead>
      <tbody>
        ${rowsHtml}
      </tbody>
    </table>
  `
  }

  <div class="footer">
    <div>Instrukcja: Odszukaj dywan w strefie zaplecza/dostaw, zawieś na wolnym ramieniu ekspozytora i zewidencjonuj w aplikacji.</div>
    <div style="font-weight: bold;">Arkusz A4 • System Zarządzania Ekspozycją Dywanów</div>
  </div>
</body>
</html>`;

  // Method 1: Hidden iframe print
  try {
    let iframe = document.getElementById('pbi-print-iframe') as HTMLIFrameElement;
    if (iframe) {
      iframe.remove();
    }
    iframe = document.createElement('iframe');
    iframe.id = 'pbi-print-iframe';
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    iframe.style.visibility = 'hidden';
    document.body.appendChild(iframe);

    const doc = iframe.contentWindow?.document;
    if (doc) {
      doc.open();
      doc.write(html);
      doc.close();
      setTimeout(() => {
        try {
          iframe.contentWindow?.focus();
          iframe.contentWindow?.print();
        } catch {
          window.print();
        }
      }, 300);
      return;
    }
  } catch (e) {
    console.warn('Iframe print failed, falling back to window.print()', e);
  }

  // Method 2: Standard window.print() fallback
  window.print();
}

/**
 * Exports missing rugs to formatted Excel workbook (.xlsx)
 */
export function exportMissingRugsToExcel(items: PowerBiStockExpoItem[], storeNumber: string) {
  const exportData = items.map((item, idx) => ({
    LP: idx + 1,
    Sklep: storeNumber,
    'REF LM': item.ref,
    'Opis dywanu': item.opis,
    'Stock Power BI': item.stock,
    Dział: '12',
    Poddział: '05',
    'Zaproponowany regał': '',
    Powieszono: ''
  }));

  const worksheet = XLSX.utils.json_to_sheet(exportData);
  worksheet['!cols'] = [
    { wch: 6 },
    { wch: 8 },
    { wch: 12 },
    { wch: 45 },
    { wch: 14 },
    { wch: 8 },
    { wch: 10 },
    { wch: 22 },
    { wch: 14 }
  ];

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Braki do powieszenia');

  const today = new Date().toISOString().split('T')[0];
  XLSX.writeFile(workbook, `Braki_Stock_Expo_Sklep_${storeNumber}_${today}.xlsx`);
}
