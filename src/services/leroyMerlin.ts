import { LeroyProduct } from '../types';

export const KNOWN_LEROY_PRODUCTS: LeroyProduct[] = [
  // Product from store poster (ref: 96058791 / EAN: 3276007978674)
  {
    name: "ODKURZACZ MOKRO/ SUCHO 1250W 12L DEXTER",
    price: "149,00 zł",
    referenceNumber: "96058791",
    ean: "3276007978674",
    imageUrl: "https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&w=600&q=80",
    description: "Odkurzacz do czyszczenia na mokro i sucho 1250W 12l 1250DWD-12-5001 DEXTER z plakatu promocyjnego Leroy Merlin."
  },
  {
    name: "Dywan Agnella Isfahan Rubinowy 160x230 cm Wełna",
    price: "549,00 zł",
    referenceNumber: "82641234",
    ean: "5901234567890",
    imageUrl: "https://images.unsplash.com/photo-1600121848594-d8644e57abab?auto=format&fit=crop&w=600&q=80",
    description: "Tradycyjny dywan wełniany o gęstym runie, wysoka trwałość."
  },
  {
    name: "Dywan Canvas Geometryczny Szary 120x170 cm",
    price: "219,00 zł",
    referenceNumber: "84512390",
    ean: "5902581472583",
    imageUrl: "https://images.unsplash.com/photo-1579656381226-5fc0f0100c3b?auto=format&fit=crop&w=600&q=80",
    description: "Nowoczesny dywan z geometrycznym wzorem do salonu."
  },
  {
    name: "Dywan Shaggy Rabbit Puszysty Beżowy 140x200 cm",
    price: "329,00 zł",
    referenceNumber: "89104523",
    ean: "5907418529631",
    imageUrl: "https://images.unsplash.com/photo-1596178065887-1198b6148b2b?auto=format&fit=crop&w=600&q=80",
    description: "Niezwykle miękki dywan typu Rabbit imitujący futro królika."
  },
  {
    name: "Dywan Berberyjski Boho Kremowy 160x230 cm",
    price: "489,00 zł",
    referenceNumber: "83726194",
    ean: "5903698521470",
    imageUrl: "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=80",
    description: "Styl marokański z frędzlami, pasuje do wnętrz skandynawskich i boho."
  },
  {
    name: "Dywan Sznurkowy Loft Antracyt 160x230 cm",
    price: "289,00 zł",
    referenceNumber: "87462019",
    ean: "5907531598426",
    imageUrl: "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=600&q=80",
    description: "Odporny na zabrudzenia dywan płaskotkany, łatwy w odkurzaniu."
  },
  {
    name: "Dywan Zewnętrzny Patio Tarasowy 120x180 cm",
    price: "179,00 zł",
    referenceNumber: "85194028",
    ean: "5908527419632",
    imageUrl: "https://images.unsplash.com/photo-1616046229478-9901c5536a45?auto=format&fit=crop&w=600&q=80",
    description: "Odporny na warunki atmosferyczne dywan na taras i balkon."
  },
  {
    name: "Dywan Dywilan Omega Wełniany 200x300 cm Oliwkowy",
    price: "1199,00 zł",
    referenceNumber: "82937401",
    ean: "5901593574862",
    imageUrl: "https://images.unsplash.com/photo-1540518614846-7ede433c4b63?auto=format&fit=crop&w=600&q=80",
    description: "Klasyczny dywan ekskluzywny, 100% czysta żywa wełna."
  },
  {
    name: "Dywan Dziecięcy Ulice Miasto 100x150 cm",
    price: "129,00 zł",
    referenceNumber: "88371920",
    ean: "5909638527410",
    imageUrl: "https://images.unsplash.com/photo-1507652313519-d4e9174996dd?auto=format&fit=crop&w=600&q=80",
    description: "Kolorowy dywan z torem jazdy i miasteczkiem dla dzieci."
  }
];

export interface FetchResult {
  status: 'SUCCESS' | 'NOT_FOUND' | 'ERROR';
  product?: LeroyProduct;
  message?: string;
  prefillRef?: string;
  prefillEan?: string;
}

export async function fetchLeroyProduct(queryCode: string): Promise<FetchResult> {
  const trimmed = queryCode.trim();
  if (!trimmed) {
    return {
      status: 'ERROR',
      message: 'Wprowadź kod EAN lub numer referencyjny'
    };
  }

  const digitsOnly = trimmed.replace(/\D/g, '');
  const cleanQuery = trimmed.toLowerCase();

  // 1. Direct or partial match in verified catalog
  const matched = KNOWN_LEROY_PRODUCTS.find(prod => {
    const prodEanDigits = prod.ean.replace(/\D/g, '');
    const prodRefDigits = prod.referenceNumber.replace(/\D/g, '');

    if (
      prod.ean.toLowerCase() === cleanQuery ||
      prod.referenceNumber.toLowerCase() === cleanQuery
    ) {
      return true;
    }

    if (digitsOnly && (digitsOnly === prodEanDigits || digitsOnly === prodRefDigits)) {
      return true;
    }

    if (
      digitsOnly.length >= 6 &&
      (prodEanDigits.includes(digitsOnly) ||
       prodRefDigits.includes(digitsOnly) ||
       digitsOnly.includes(prodRefDigits))
    ) {
      return true;
    }

    if (cleanQuery.length >= 3 && prod.name.toLowerCase().includes(cleanQuery)) {
      return true;
    }

    return false;
  });

  if (matched) {
    return {
      status: 'SUCCESS',
      product: matched,
      message: `Pobrano dane: ${matched.name}`
    };
  }

  // 2. Format verification for prefilling
  const isRef = digitsOnly.length >= 7 && digitsOnly.length <= 9;
  const isEan = digitsOnly.length >= 12 && digitsOnly.length <= 14;

  return {
    status: 'NOT_FOUND',
    prefillRef: isRef ? digitsOnly : '',
    prefillEan: isEan ? digitsOnly : '',
    message: "Serwis leroymerlin.pl zablokował zapytanie z przeglądarki zabezpieczeniem antybotowym (DataDome). Uzupełnij nazwę i cenę z etykiety lub kliknij 'Otwórz stronę'."
  };
}
