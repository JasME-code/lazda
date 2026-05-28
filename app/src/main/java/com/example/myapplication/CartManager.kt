package com.example.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CartItem(
    val product: Product,
    var quantity: Int = 1
)

object CartManager {

    private const val PREF_NAME = "cart_prefs"
    private const val KEY_CART  = "cart_items"

    // ✅ Now stores CartItem (product + quantity) instead of just Product
    val cartItems = mutableListOf<CartItem>()

    // Keep this for backward compatibility with existing code that uses cartList
    val cartList: List<Product> get() = cartItems.map { it.product }

    fun loadCart(context: Context) {
        cartItems.clear()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_CART, null) ?: return
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val product = Product(
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
                        ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
                    isRestricted = obj.optBoolean("isRestricted", false),
                    sellerUid    = obj.optString("sellerUid", ""),
                    firebaseKey  = obj.optString("firebaseKey", "")
                )
                cartItems.add(CartItem(product, obj.optInt("quantity", 1)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cartItems.clear()
        }
    }

    fun saveCart(context: Context) {
        val array = JSONArray()
        cartItems.forEach { cartItem ->
            val obj = JSONObject().apply {
                put("id",           cartItem.product.id)
                put("name",         cartItem.product.name)
                put("price",        cartItem.product.price)
                put("rating",       cartItem.product.rating)
                put("seller",       cartItem.product.seller)
                put("imageRes",     cartItem.product.imageRes)
                put("category",     cartItem.product.category)
                put("stock",        cartItem.product.stock)
                put("material",     cartItem.product.material)
                put("usage",        cartItem.product.usage)
                put("details",      JSONArray(cartItem.product.details))
                put("isRestricted", cartItem.product.isRestricted)
                put("sellerUid",    cartItem.product.sellerUid)
                put("firebaseKey",  cartItem.product.firebaseKey)
                put("quantity",     cartItem.quantity)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CART, array.toString()).apply()
    }

    fun addToCart(context: Context, product: Product) {
        val existing = cartItems.find { it.product.firebaseKey == product.firebaseKey }
        if (existing == null) {
            cartItems.add(CartItem(product, 1))
        } else {
            existing.quantity++
        }
        saveCart(context)
    }

    fun removeFromCart(context: Context, product: Product) {
        cartItems.removeAll { it.product.firebaseKey == product.firebaseKey }
        saveCart(context)
    }

    fun increaseQty(context: Context, firebaseKey: String) {
        val item = cartItems.find { it.product.firebaseKey == firebaseKey } ?: return
        if (item.quantity < item.product.stock) {
            item.quantity++
            saveCart(context)
        }
    }

    fun decreaseQty(context: Context, firebaseKey: String) {
        val item = cartItems.find { it.product.firebaseKey == firebaseKey } ?: return
        if (item.quantity > 1) {
            item.quantity--
            saveCart(context)
        } else {
            // quantity would go to 0 — remove item entirely
            cartItems.removeAll { it.product.firebaseKey == firebaseKey }
            saveCart(context)
        }
    }

    fun clearCart(context: Context) {
        cartItems.clear()
        saveCart(context)
    }

    fun getTotalPrice(): Double = cartItems.sumOf { it.product.price * it.quantity }
    fun getTotalItems(): Int    = cartItems.sumOf { it.quantity }

    fun isInCart(product: Product): Boolean =
        cartItems.any { it.product.firebaseKey == product.firebaseKey }
}