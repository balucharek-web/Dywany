"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.transferSuperAdminRole = exports.scheduledProductUpdate = exports.getProductByLMNumber = exports.getProductByEAN = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
const leroyMerlinService_1 = require("./leroyMerlinService");
admin.initializeApp();
const db = admin.firestore();
const MASTER_SUPER_ADMIN_EMAIL = "baluch.arek@gmail.com";
/**
 * Callable function: Search product by EAN.
 * Checks Firestore cache first; if missing or outdated, queries Leroy Merlin website.
 */
exports.getProductByEAN = functions.https.onCall(async (data, context) => {
    const ean = (data.ean || "").trim();
    if (!ean) {
        throw new functions.https.HttpsError("invalid-argument", "Kod EAN jest wymagany.");
    }
    // 1. Check existing product in Firestore
    const snap = await db.collection("products").where("ean", "==", ean).limit(1).get();
    if (!snap.empty) {
        const existing = snap.docs[0].data();
        const lastUpdated = existing.lastUpdated?.toDate();
        const now = new Date();
        const hoursSinceUpdate = lastUpdated ? (now.getTime() - lastUpdated.getTime()) / (1000 * 3600) : 999;
        // If updated in the last 24 hours, return cached
        if (hoursSinceUpdate < 24) {
            return { success: true, source: "cache", product: { id: snap.docs[0].id, ...existing } };
        }
    }
    // 2. Fetch from Leroy Merlin
    const lmData = await (0, leroyMerlinService_1.fetchProductFromLM)(ean, true);
    if (!lmData) {
        if (!snap.empty) {
            return { success: true, source: "stale_cache", product: { id: snap.docs[0].id, ...snap.docs[0].data() } };
        }
        return { success: false, message: "Nie udało się pobrać danych produktu z Leroy Merlin." };
    }
    // 3. Save or update in Firestore (respecting localPriceOverride!)
    let docRef;
    let localPrice = 0;
    let localPriceOverride = false;
    if (!snap.empty) {
        docRef = snap.docs[0].ref;
        const existing = snap.docs[0].data();
        localPrice = existing.localPrice || 0;
        localPriceOverride = existing.localPriceOverride || false;
    }
    else {
        docRef = db.collection("products").doc(lmData.lmSystemNumber ? `lm_${lmData.lmSystemNumber}` : `ean_${ean}`);
    }
    const payload = {
        productId: docRef.id,
        name: lmData.name,
        ean: lmData.ean || ean, // NEVER truncate EAN
        lmSystemNumber: lmData.lmSystemNumber,
        onlinePrice: lmData.onlinePrice,
        localPrice: localPrice,
        localPriceOverride: localPriceOverride,
        imageUrl: lmData.imageUrl,
        productUrl: lmData.productUrl,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    };
    await docRef.set(payload, { merge: true });
    return { success: true, source: "live_fetch", product: payload };
});
/**
 * Callable function: Search product by LM system number.
 */
exports.getProductByLMNumber = functions.https.onCall(async (data, context) => {
    const lmNumber = (data.lmNumber || "").trim();
    if (!lmNumber) {
        throw new functions.https.HttpsError("invalid-argument", "Numer systemowy Leroy Merlin jest wymagany.");
    }
    const snap = await db.collection("products").where("lmSystemNumber", "==", lmNumber).limit(1).get();
    if (!snap.empty) {
        const existing = snap.docs[0].data();
        return { success: true, source: "cache", product: { id: snap.docs[0].id, ...existing } };
    }
    const lmData = await (0, leroyMerlinService_1.fetchProductFromLM)(lmNumber, false);
    if (!lmData) {
        return { success: false, message: "Nie udało się pobrać danych produktu z Leroy Merlin." };
    }
    const docRef = db.collection("products").doc(`lm_${lmNumber}`);
    const payload = {
        productId: docRef.id,
        name: lmData.name,
        ean: lmData.ean,
        lmSystemNumber: lmNumber,
        onlinePrice: lmData.onlinePrice,
        localPrice: 0,
        localPriceOverride: false,
        imageUrl: lmData.imageUrl,
        productUrl: lmData.productUrl,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    };
    await docRef.set(payload, { merge: true });
    return { success: true, source: "live_fetch", product: payload };
});
/**
 * Scheduled sync task: Updates online prices every 24 hours.
 * CRITICAL RULE: If localPriceOverride === true, localPrice is NEVER changed!
 */
exports.scheduledProductUpdate = functions.pubsub.schedule("every 24 hours").onRun(async (context) => {
    const productsSnap = await db.collection("products").get();
    console.log(`Rozpoczęto zaplanowaną aktualizację ${productsSnap.size} produktów.`);
    for (const doc of productsSnap.docs) {
        const p = doc.data();
        const idToSearch = p.lmSystemNumber || p.ean;
        if (!idToSearch)
            continue;
        try {
            const freshData = await (0, leroyMerlinService_1.fetchProductFromLM)(idToSearch, !p.lmSystemNumber);
            if (freshData && freshData.onlinePrice > 0) {
                const updatePayload = {
                    onlinePrice: freshData.onlinePrice,
                    name: freshData.name || p.name,
                    imageUrl: freshData.imageUrl || p.imageUrl,
                    lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                };
                // Note: localPrice and localPriceOverride are completely untouched!
                await doc.ref.update(updatePayload);
                // If localPriceOverride is false, also update current price on active display spot
                if (!p.localPriceOverride) {
                    const assSnap = await db.collection("displayAssignments").where("productId", "==", doc.id).get();
                    for (const assDoc of assSnap.docs) {
                        await assDoc.ref.update({
                            price: freshData.onlinePrice,
                            productName: freshData.name || p.name,
                            updatedAt: admin.firestore.FieldValue.serverTimestamp()
                        });
                    }
                }
            }
        }
        catch (err) {
            console.warn(`Błąd aktualizacji produktu ${doc.id}:`, err.message);
        }
    }
    console.log("Zakończono zaplanowaną aktualizację produktów.");
});
/**
 * Secure backend function to transfer SUPER_ADMIN role atomicaly.
 */
exports.transferSuperAdminRole = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Wymagane uwierzytelnienie.");
    }
    const callerUid = context.auth.uid;
    const callerSnap = await db.collection("users").doc(callerUid).get();
    const callerData = callerSnap.data();
    const isMasterCaller = context.auth.token.email?.toLowerCase() === MASTER_SUPER_ADMIN_EMAIL.toLowerCase();
    const isSuperAdminCaller = callerData?.role === "SUPER_ADMIN" || isMasterCaller;
    if (!isSuperAdminCaller) {
        throw new functions.https.HttpsError("permission-denied", "Tylko SUPER_ADMIN może przekazać rolę.");
    }
    const newSuperAdminUid = data.newSuperAdminUid;
    if (!newSuperAdminUid) {
        throw new functions.https.HttpsError("invalid-argument", "Należy wskazać użytkownika.");
    }
    const targetRef = db.collection("users").doc(newSuperAdminUid);
    const callerRef = db.collection("users").doc(callerUid);
    await db.runTransaction(async (transaction) => {
        const targetDoc = await transaction.get(targetRef);
        if (!targetDoc.exists) {
            throw new functions.https.HttpsError("not-found", "Użytkownik docelowy nie istnieje.");
        }
        transaction.update(targetRef, {
            role: "SUPER_ADMIN",
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        transaction.update(callerRef, {
            role: "ADMIN",
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
    });
    return { success: true };
});
//# sourceMappingURL=index.js.map