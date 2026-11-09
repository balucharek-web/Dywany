export interface Carpet {
  id: string; // e.g. "mag_1_A" or doc ID
  magazyn: number; // 1, 2, 3...
  podmagazyn: string; // "A", "B", etc.
  miejsce: string; // e.g. "1 A", "20 A"
  nazwa: string;
  ref: string;
  ean: string;
  dodano: string; // e.g. "2026-06-26"
  updatedAt?: string;
  updatedBy?: string;
  sklad?: string;
  wysokoscRuna?: string;
  grubosc?: string;
  materialGlowny?: string;
  tekstura?: string;
  miejsceUzytkowania?: string;
  marka?: string;
  model?: string;
  spodAntyposlizgowy?: string;
  wlasciwosci?: string;
  przeznaczenie?: string;
}

export interface ProductCatalogItem {
  id: string; // e.g. "ean_5905549008430" or "ref_82850488"
  nazwa: string;
  ref?: string;
  ean?: string;
  cena?: string;
  updatedAt?: string;
  updatedBy?: string;
  source?: 'seed' | 'user' | 'esl_ocr' | 'import';
}

export interface AdminUser {
  email: string;
  role: 'superadmin' | 'admin';
  addedBy?: string;
  createdAt: string;
}

export interface Warehouse {
  id: string; // e.g. "15" or doc id
  number: number;
  name?: string;
  createdAt?: string;
  createdBy?: string;
}

export type ActivityAction = 'add' | 'edit' | 'delete';

export interface ActivityLog {
  id: string;
  timestamp: string; // ISO string
  userEmail: string;
  userName?: string;
  action: ActivityAction;
  target: string;
  details: string;
  miejsce?: string;
  ref?: string;
  ean?: string;
}

export const SUPER_ADMIN_EMAIL = 'baluch.arek@gmail.com';
export const STORE_NUMBER = '038';
export const STORE_CITY = 'MIKOŁÓW';
export const STORE_NAME = 'Sklep 038 MIKOŁÓW';

export interface PowerBiStockExpoItem {
  id: string; // ref LM
  ref: string;
  opis: string;
  stock: number;
  sklep?: string;
  dzial?: string;
  poddzial?: string;
  typ?: string;
  podtyp?: string;
}

export interface PowerBiSyncState {
  items: PowerBiStockExpoItem[];
  updatedAt: string;
  updatedBy: string;
  totalCount: number;
}

export const POWER_BI_STOCK_EXPO_URL = 'https://app.powerbi.com/groups/e90c2306-1d07-41e2-861c-0d2af37d4e43/reports/5a86aebb-49ef-424c-a32e-03dc0fb96aa3/ReportSection?ctid=9f1db594-b8ab-4e59-b56b-4a3c145eaf07&pbi_source=shareVisual&visual=fcac75d2e8822d0a0519&height=613.30&width=739.08&bookmarkGuid=4274b0fd-cc1c-4b66-96b9-50b7be54a021';
export const POWER_BI_EMBED_URL = 'https://app.powerbi.com/reportEmbed?reportId=5a86aebb-49ef-424c-a32e-03dc0fb96aa3&groupId=e90c2306-1d07-41e2-861c-0d2af37d4e43&ctid=9f1db594-b8ab-4e59-b56b-4a3c145eaf07&bookmarkGuid=4274b0fd-cc1c-4b66-96b9-50b7be54a021&autoAuth=true';
