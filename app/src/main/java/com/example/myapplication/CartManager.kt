package com.example.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CartManager {

    private const val PREF_NAME = "cart_prefs"
    private const val KEY_CART  = "cart_items"

    val cartList = mutableListOf<Product>()

    // Call on app start or after login to restore cart
    fun loadCart(context: Context) {
        cartList.clear()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_CART, null) ?: return

        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                cartList.add(
                    Product(
                        id           = obj.optInt("id", 0),
                        name         = obj.optString("name", ""),
                        price        = obj.optDouble("price", 0.0),
                        rating       = obj.optDouble("rating", 0.0).toFloat(),
                        seller       = obj.optString("seller", ""),
                        imageRes     = obj.optInt("imageRes", 0),
                        category     = obj.optString("category", ""),
                        stock        = obj.optInt("stock", 0),
                        material     = obj.optString("material", ""),
                        usage        = obj.optString("usage", ""),
                        details      = obj.optJSONArray("details")
                            ?.let { arr ->
                                (0 until arr.length()).map { arr.getString(it) }
                            } ?: emptyList(),
                        isRestricted = obj.optBoolean("isRestricted", false),
                        sellerUid    = obj.optString("sellerUid", ""),
                        firebaseKey  = obj.optString("firebaseKey", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cartList.clear()
        }
    }

    // Call after every add/remove to persist cart
    fun saveCart(context: Context) {
        val array = JSONArray()
        cartList.forEach { product ->
            val obj = JSONObject().apply {
                put("id",           product.id)
                put("name",         product.name)
                put("price",        product.price)
                put("rating",       product.rating)
                put("seller",       product.seller)
                put("imageRes",     product.imageRes)
                put("category",     product.category)
                put("stock",        product.stock)
                put("material",     product.material)
                put("usage",        product.usage)
                put("details",      JSONArray(product.details))
                put("isRestricted", product.isRestricted)
                put("sellerUid",    product.sellerUid)
                put("firebaseKey",  product.firebaseKey)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CART, array.toString())
            .apply()
    }

    fun addToCart(context: Context, product: Product) {
        val existing = cartList.find { it.firebaseKey == product.firebaseKey }
        if (existing == null) {
            cartList.add(product)
            saveCart(context)
        }
    }

    fun removeFromCart(context: Context, product: Product) {
        cartList.removeAll { it.firebaseKey == product.firebaseKey }
        saveCart(context)
    }

    fun clearCart(context: Context) {
        cartList.clear()
        saveCart(context)
    }

    fun getTotalPrice(): Double = cartList.sumOf { it.price }

    fun isInCart(product: Product): Boolean =
        cartList.any { it.firebaseKey == product.firebaseKey }
}