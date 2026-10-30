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

export type ActivityAction =
  | 'add'
  | 'edit'
  | 'delete'
  | 'import'
  | 'add_warehouse'
  | 'delete_warehouse'
  | 'add_admin'
  | 'remove_admin';

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
