package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// FIX #6 & #7: Added sellerUid and firebaseKey fields to Product
@Parcelize
data class Product(
    val id          : Int     = 0,
    val name        : String  = "",
    val price       : Double  = 0.0,
    val rating      : Float   = 0f,
    val seller      : String  = "",
    val imageRes    : Int     = 0,
    val category    : String  = "",
    val stock       : Int     = 0,
    val material    : String  = "",
    val usage       : String  = "",
    val details     : List<String> = emptyList(),
    val isRestricted: Boolean = false,
    val sellerUid   : String  = "",   // UID of the seller who added this product
    val firebaseKey : String  = ""    // Firebase push key (used for edit/delete)
) : Parcelable

@Parcelize
data class Order(
    val orderId      : String       = "",
    val items        : List<Product> = emptyList(),
    val totalPrice   : Double       = 0.0,
    val address      : String       = "",
    val paymentMethod: String       = "",
    val date         : String       = "",
    var status       : String       = "Pending"
) : Parcelable

// In-memory cart only — intentional (session-based cart)
object CartManager {
    val cartList = mutableListOf<Product>()
}
