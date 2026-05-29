package com.example.myapplication

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Product(
    val id          : Int          = 0,
    val name        : String       = "",
    val price       : Double       = 0.0,
    val rating      : Float        = 0f,
    val seller      : String       = "",
    val imageRes    : Int          = 0,
    val category    : String       = "",
    val stock       : Int          = 0,
    val material    : String       = "",
    val usage       : String       = "",
    val details     : List<String> = emptyList(),
    val isRestricted: Boolean      = false,  // kept for backward compat
    // ✅ NEW: minAge = 0 means anyone can see it, 18 = only 18+ customers see it
    val minAge      : Int          = 0,
    val sellerUid   : String       = "",
    val firebaseKey : String       = ""
) : Parcelable {

    fun toFirebaseMap(): Map<String, Any> = mapOf(
        "id"           to id,
        "name"         to name,
        "price"        to price,
        "rating"       to rating,
        "seller"       to seller,
        "category"     to category,
        "stock"        to stock,
        "material"     to material,
        "usage"        to usage,
        "details"      to details,
        "isRestricted" to (minAge >= 18),
        "minAge"       to minAge,
        "sellerUid"    to sellerUid,
        "firebaseKey"  to firebaseKey
    )

    companion object {
        fun fromFirebaseMap(map: Map<String, Any?>, key: String): Product {
            return Product(
                id           = (map["id"] as? Long)?.toInt() ?: 0,
                name         = map["name"] as? String ?: "",
                price        = (map["price"] as? Double) ?: (map["price"] as? Long)?.toDouble() ?: 0.0,
                rating       = (map["rating"] as? Double)?.toFloat() ?: (map["rating"] as? Long)?.toFloat() ?: 0f,
                seller       = map["seller"] as? String ?: "",
                imageRes     = 0,
                category     = map["category"] as? String ?: "",
                stock        = (map["stock"] as? Long)?.toInt() ?: 0,
                material     = map["material"] as? String ?: "",
                usage        = map["usage"] as? String ?: "",
                details      = (map["details"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                isRestricted = map["isRestricted"] as? Boolean ?: false,
                minAge       = (map["minAge"] as? Long)?.toInt() ?: 0,
                sellerUid    = map["sellerUid"] as? String ?: "",
                firebaseKey  = key
            )
        }
    }
}