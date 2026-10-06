package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class SyncPayload(
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long,
    val carpets: List<Carpet>,
    val stands: List<DisplayStand>,
    val orders: List<TakeDownOrder> = emptyList()
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("deviceId", deviceId)
        root.put("deviceName", deviceName)
        root.put("timestamp", timestamp)

        val carpetsArray = JSONArray()
        for (carpet in carpets) {
            val c = JSONObject()
            c.put("id", carpet.id)
            c.put("barcode", carpet.barcode)
            c.put("name", carpet.name)
            c.put("size", carpet.size)
            c.put("collection", carpet.collection)
            c.put("composition", carpet.composition)
            c.put("pricePln", carpet.pricePln)
            if (carpet.promoPricePln != null) c.put("promoPricePln", carpet.promoPricePln) else c.put("promoPricePln", JSONObject.NULL)
            if (carpet.currentStandId != null) c.put("currentStandId", carpet.currentStandId) else c.put("currentStandId", JSONObject.NULL)
            if (carpet.currentSlot != null) c.put("currentSlot", carpet.currentSlot) else c.put("currentSlot", JSONObject.NULL)
            c.put("status", carpet.status.name)
            c.put("patternType", carpet.patternType)
            c.put("notes", carpet.notes)
            if (carpet.displaySinceTimestamp != null) c.put("displaySinceTimestamp", carpet.displaySinceTimestamp) else c.put("displaySinceTimestamp", JSONObject.NULL)
            if (carpet.reservedForName != null) c.put("reservedForName", carpet.reservedForName) else c.put("reservedForName", JSONObject.NULL)
            if (carpet.reservedPhone != null) c.put("reservedPhone", carpet.reservedPhone) else c.put("reservedPhone", JSONObject.NULL)
            if (carpet.reservedUntilTime != null) c.put("reservedUntilTime", carpet.reservedUntilTime) else c.put("reservedUntilTime", JSONObject.NULL)
            c.put("updatedAt", carpet.updatedAt)
            carpetsArray.put(c)
        }
        root.put("carpets", carpetsArray)

        val standsArray = JSONArray()
        for (stand in stands) {
            val s = JSONObject()
            s.put("id", stand.id)
            s.put("code", stand.code)
            s.put("name", stand.name)
            s.put("section", stand.section)
            s.put("barcode", stand.barcode)
            if (stand.slot1CarpetId != null) s.put("slot1CarpetId", stand.slot1CarpetId) else s.put("slot1CarpetId", JSONObject.NULL)
            if (stand.slot2CarpetId != null) s.put("slot2CarpetId", stand.slot2CarpetId) else s.put("slot2CarpetId", JSONObject.NULL)
            s.put("notes", stand.notes)
            s.put("updatedAt", stand.updatedAt)
            standsArray.put(s)
        }
        root.put("stands", standsArray)

        val ordersArray = JSONArray()
        for (order in orders) {
            val o = JSONObject()
            o.put("id", order.id)
            o.put("carpetId", order.carpetId)
            o.put("carpetName", order.carpetName)
            o.put("carpetBarcode", order.carpetBarcode)
            o.put("carpetSize", order.carpetSize)
            o.put("standId", order.standId)
            o.put("standName", order.standName)
            o.put("standCode", order.standCode)
            o.put("slot", order.slot)
            o.put("requestedBy", order.requestedBy)
            o.put("notes", order.notes)
            o.put("isCompleted", order.isCompleted)
            if (order.completedBy != null) o.put("completedBy", order.completedBy) else o.put("completedBy", JSONObject.NULL)
            o.put("createdAt", order.createdAt)
            o.put("updatedAt", order.updatedAt)
            ordersArray.put(o)
        }
        root.put("orders", ordersArray)

        return root.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): SyncPayload {
            val root = JSONObject(jsonStr)
            val deviceId = root.optString("deviceId", "unknown")
            val deviceName = root.optString("deviceName", "Urządzenie")
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            val carpets = mutableListOf<Carpet>()
            val carpetsArray = root.optJSONArray("carpets")
            if (carpetsArray != null) {
                for (i in 0 until carpetsArray.length()) {
                    val c = carpetsArray.getJSONObject(i)
                    val statusStr = c.optString("status", CarpetStatus.IN_STORAGE.name)
                    val status = try {
                        CarpetStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        CarpetStatus.IN_STORAGE
                    }

                    carpets.add(
                        Carpet(
                            id = c.getString("id"),
                            barcode = c.optString("barcode", ""),
                            name = c.optString("name", "Dywan"),
                            size = c.optString("size", ""),
                            collection = c.optString("collection", ""),
                            composition = c.optString("composition", ""),
                            pricePln = c.optDouble("pricePln", 0.0),
                            promoPricePln = if (c.isNull("promoPricePln")) null else c.optDouble("promoPricePln"),
                            currentStandId = if (c.isNull("currentStandId")) null else c.optString("currentStandId"),
                            currentSlot = if (c.isNull("currentSlot")) null else c.optInt("currentSlot"),
                            status = status,
                            patternType = c.optInt("patternType", 0),
                            notes = c.optString("notes", ""),
                            displaySinceTimestamp = if (c.isNull("displaySinceTimestamp")) null else c.optLong("displaySinceTimestamp"),
                            reservedForName = if (c.isNull("reservedForName")) null else c.optString("reservedForName"),
                            reservedPhone = if (c.isNull("reservedPhone")) null else c.optString("reservedPhone"),
                            reservedUntilTime = if (c.isNull("reservedUntilTime")) null else c.optLong("reservedUntilTime"),
                            updatedAt = c.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val stands = mutableListOf<DisplayStand>()
            val standsArray = root.optJSONArray("stands")
            if (standsArray != null) {
                for (i in 0 until standsArray.length()) {
                    val s = standsArray.getJSONObject(i)
                    stands.add(
                        DisplayStand(
                            id = s.getString("id"),
                            code = s.optString("code", ""),
                            name = s.optString("name", "Stanowisko"),
                            section = s.optString("section", ""),
                            barcode = s.optString("barcode", ""),
                            slot1CarpetId = if (s.isNull("slot1CarpetId")) null else s.optString("slot1CarpetId"),
                            slot2CarpetId = if (s.isNull("slot2CarpetId")) null else s.optString("slot2CarpetId"),
                            notes = s.optString("notes", ""),
                            updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val orders = mutableListOf<TakeDownOrder>()
            val ordersArray = root.optJSONArray("orders")
            if (ordersArray != null) {
                for (i in 0 until ordersArray.length()) {
                    val o = ordersArray.getJSONObject(i)
                    orders.add(
                        TakeDownOrder(
                            id = o.getString("id"),
                            carpetId = o.optString("carpetId", ""),
                            carpetName = o.optString("carpetName", ""),
                            carpetBarcode = o.optString("carpetBarcode", ""),
                            carpetSize = o.optString("carpetSize", ""),
                            standId = o.optString("standId", ""),
                            standName = o.optString("standName", ""),
                            standCode = o.optString("standCode", ""),
                            slot = o.optInt("slot", 1),
                            requestedBy = o.optString("requestedBy", ""),
                            notes = o.optString("notes", ""),
                            isCompleted = o.optBoolean("isCompleted", false),
                            completedBy = if (o.isNull("completedBy")) null else o.optString("completedBy"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return SyncPayload(deviceId, deviceName, timestamp, carpets, stands, orders)
        }
    }
}
