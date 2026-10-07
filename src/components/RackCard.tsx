import React from 'react';
import { CarpetSlot } from '../types';
import { Edit2, MoveRight, Plus, QrCode, Tag, Barcode } from 'lucide-react';

interface RackCardProps {
  rackNumber: number;
  slotA: CarpetSlot;
  slotB: CarpetSlot;
  isAuthenticated: boolean;
  onEditSlot: (slot: CarpetSlot) => void;
  onAddSlot: (rackNumber: number, slotLetter: string) => void;
  onMoveSlot: (slot: CarpetSlot) => void;
  onPromptLogin: () => void;
}

export const RackCard: React.FC<RackCardProps> = ({
  rackNumber,
  slotA,
  slotB,
  isAuthenticated,
  onEditSlot,
  onAddSlot,
  onMoveSlot,
  onPromptLogin
}) => {
  const occupiedCount = (slotA.occupied ? 1 : 0) + (slotB.occupied ? 1 : 0);

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs p-4 flex flex-col space-y-4 hover:shadow-md transition">
      {/* Rack Header */}
      <div className="flex items-center justify-between pb-1 border-b border-slate-100">
        <div className="flex items-center space-x-2.5">
          <span className="px-2.5 py-1 rounded-lg bg-blue-700 text-white font-extrabold text-xs tracking-wide">
            STOJAK {rackNumber}
          </span>
          <span className="font-bold text-sm text-slate-800">
            Magazyn {rackNumber}
          </span>
        </div>
        <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-2.5 py-0.5 rounded-full">
          {occupiedCount} / 2 dywany
        </span>
      </div>

      {/* 2 Slots Side-by-Side: Slot A and Slot B */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 flex-1">
        <SlotItemView
          slot={slotA}
          isAuthenticated={isAuthenticated}
          onEditSlot={onEditSlot}
          onAddSlot={onAddSlot}
          onMoveSlot={onMoveSlot}
          onPromptLogin={onPromptLogin}
        />
        <SlotItemView
          slot={slotB}
          isAuthenticated={isAuthenticated}
          onEditSlot={onEditSlot}
          onAddSlot={onAddSlot}
          onMoveSlot={onMoveSlot}
          onPromptLogin={onPromptLogin}
        />
      </div>
    </div>
  );
};

interface SlotItemViewProps {
  slot: CarpetSlot;
  isAuthenticated: boolean;
  onEditSlot: (slot: CarpetSlot) => void;
  onAddSlot: (rackNumber: number, slotLetter: string) => void;
  onMoveSlot: (slot: CarpetSlot) => void;
  onPromptLogin: () => void;
}

const SlotItemView: React.FC<SlotItemViewProps> = ({
  slot,
  isAuthenticated,
  onEditSlot,
  onAddSlot,
  onMoveSlot,
  onPromptLogin
}) => {
  const isOccupied = slot.occupied;

  return (
    <div
      className={`rounded-xl border p-3.5 flex flex-col justify-between transition ${
        isOccupied
          ? 'bg-white border-slate-200 shadow-xs'
          : 'bg-slate-50/70 border-dashed border-slate-200'
      }`}
    >
      <div>
        {/* Slot Badge and Status indicator */}
        <div className="flex items-center justify-between mb-2.5">
          <span
            className={`px-2 py-0.5 rounded-md font-extrabold text-[11px] uppercase tracking-wider ${
              isOccupied
                ? 'bg-blue-100 text-blue-800'
                : 'bg-slate-200 text-slate-600'
            }`}
          >
            Miejsce {slot.slotId.toUpperCase()}
          </span>

          <div className="flex items-center space-x-1.5">
            <span
              className={`w-2 h-2 rounded-full ${
                isOccupied ? 'bg-blue-600' : 'bg-slate-300'
              }`}
            />
            <span
              className={`text-[11px] font-semibold ${
                isOccupied ? 'text-blue-700' : 'text-slate-400'
              }`}
            >
              {isOccupied ? 'Zajęte' : 'Wolne'}
            </span>
          </div>
        </div>

        {isOccupied ? (
          <div className="space-y-2">
            {/* Optional Carpet Image */}
            {slot.imageUrl && (
              <img
                src={slot.imageUrl}
                alt={slot.productName}
                className="w-full h-24 rounded-lg object-cover border border-slate-100"
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
            )}

            {/* Product Title */}
            <h4
              className="text-xs font-bold text-slate-900 line-clamp-2 leading-snug"
              title={slot.productName}
            >
              {slot.productName || 'Dywan bez nazwy'}
            </h4>

            {/* Price */}
            {slot.price && (
              <div className="text-sm font-extrabold text-blue-700">
                {slot.price}
              </div>
            )}

            {/* ESL, Ref, and EAN tags */}
            <div className="space-y-1 pt-1 text-[11px]">
              {slot.eslCode && (
                <div className="inline-flex items-center space-x-1 px-1.5 py-0.5 rounded-md bg-blue-50 text-blue-800 font-semibold border border-blue-100">
                  <QrCode className="w-3 h-3 text-blue-600 shrink-0" />
                  <span>{slot.eslCode}</span>
                </div>
              )}

              {slot.referenceNumber && (
                <div className="flex items-center space-x-1 text-slate-600">
                  <Tag className="w-3 h-3 text-slate-400 shrink-0" />
                  <span>Ref: {slot.referenceNumber}</span>
                </div>
              )}

              {slot.ean && (
                <div className="flex items-center space-x-1 text-slate-500 font-mono text-[10px]">
                  <Barcode className="w-3 h-3 text-slate-400 shrink-0" />
                  <span>EAN: {slot.ean}</span>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="h-28 flex items-center justify-center rounded-lg bg-slate-100/50 border border-slate-100 text-xs text-slate-400 font-medium">
            Miejsce {slot.slotLetter.toUpperCase()} wolne
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div className="pt-3 mt-3 border-t border-slate-100">
        {isOccupied ? (
          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                if (isAuthenticated) onEditSlot(slot);
                else onPromptLogin();
              }}
              className="flex-1 flex items-center justify-center space-x-1.5 py-1.5 px-2.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition cursor-pointer"
            >
              <Edit2 className="w-3.5 h-3.5 text-blue-700" />
              <span>Edytuj</span>
            </button>
            <button
              onClick={() => {
                if (isAuthenticated) onMoveSlot(slot);
                else onPromptLogin();
              }}
              title="Przenieś dywan na inny stojak"
              className="p-1.5 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 transition cursor-pointer border border-blue-200"
            >
              <MoveRight className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <button
            onClick={() => {
              if (isAuthenticated) onAddSlot(slot.rackNumber, slot.slotLetter);
              else onPromptLogin();
            }}
            className="w-full flex items-center justify-center space-x-1.5 py-2 px-3 rounded-lg bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs shadow-xs transition cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Dodaj dywan</span>
          </button>
        )}
      </div>
    </div>
  );
};
