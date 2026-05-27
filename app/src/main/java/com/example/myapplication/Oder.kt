package com.example.myapplication

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Order(
    val orderId      : String       = "",
    val items        : List<Product> = emptyList(),
    val totalPrice   : Double       = 0.0,
    val address      : String       = "",
    val paymentMethod: String       = "",
    val date         : String       = "",
    var status       : String       = "Pending",
    val buyerUid     : String       = ""
) : Parcelable {

    // Convert to Firebase-safe map (no imageRes, no Parcelable fields)
    fun toFirebaseMap(): Map<String, Any> = mapOf(
        "orderId"       to orderId,
        "items"         to items.map { it.toFirebaseMap() },
        "totalPrice"    to totalPrice,
        "address"       to address,
        "paymentMethod" to paymentMethod,
        "date"          to date,
        "status"        to status,
        "buyerUid"      to buyerUid
    )

    companion object {
        // Safe deserializer from Firebase snapshot map
        @Suppress("UNCHECKED_CAST")
        fun fromFirebaseMap(map: Map<String, Any?>): Order {
            val rawItems = map["items"]
            val products = when (rawItems) {
                is List<*> -> rawItems.mapNotNull { item ->
                    (item as? Map<String, Any?>)?.let { itemMap ->
                        val key = itemMap["firebaseKey"] as? String ?: ""
                        Product.fromFirebaseMap(itemMap, key)
                    }
                }
                is Map<*, *> -> (rawItems as Map<String, Any?>).values.mapNotNull { item ->
                    (item as? Map<String, Any?>)?.let { itemMap ->
                        val key = itemMap["firebaseKey"] as? String ?: ""
                        Product.fromFirebaseMap(itemMap, key)
                    }
                }
                else -> emptyList()
            }

            return Order(
                orderId       = map["orderId"] as? String ?: "",
                items         = products,
                totalPrice    = (map["totalPrice"] as? Double)
                    ?: (map["totalPrice"] as? Long)?.toDouble() ?: 0.0,
                address       = map["address"] as? String ?: "",
                paymentMethod = map["paymentMethod"] as? String ?: "",
                date          = map["date"] as? String ?: "",
                status        = map["status"] as? String ?: "Pending",
                buyerUid      = map["buyerUid"] as? String ?: ""
            )
        }
    }
}