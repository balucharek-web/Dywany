export interface CarpetSlot {
  slotId: string;
  rackNumber: number;
  slotLetter: 'a' | 'b' | string;
  occupied: boolean;
  productName: string;
  ean: string;
  referenceNumber: string;
  eslCode: string;
  price: string;
  imageUrl: string;
  updatedBy?: string;
  updatedAt?: any;
  createdAt?: any;
}

export type SlotFilter = 'ALL' | 'OCCUPIED' | 'EMPTY';

export type RackRange = 'ALL' | '1-10' | '11-20' | '21-30';

export interface StoreStats {
  totalRacks: number;
  totalSlots: number;
  occupiedCount: number;
  emptyCount: number;
}

export interface LeroyProduct {
  name: string;
  price: string;
  referenceNumber: string;
  ean: string;
  imageUrl: string;
  description?: string;
}

export interface UserProfile {
  uid: string;
  displayName: string | null;
  email: string | null;
  photoURL?: string | null;
}
