package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {

    private lateinit var tvTotal: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: Dashboard.ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitycart)

        // 1. Setup Views
        tvTotal    = findViewById(R.id.tvCartTotal)
        tvSubtotal = findViewById(R.id.tvCartSubtotal)
        listView   = findViewById(R.id.lvCartItems)

        // 2. Setup ListView with in-memory CartManager
        // CartManager.cartList is populated when user taps products on Dashboard.
        // Cart is intentionally in-memory (session-based), meaning it resets
        // after app close — this matches common e-commerce UX for guest/session carts.
        // If you want a persistent cart tied to a Firebase user, see the comment below.
        adapter = Dashboard.ProductAdapter(this, CartManager.cartList)
        listView.adapter = adapter

        updateTotals()

        // 3. Back Button
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        // 4. Long press to remove item
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val removedItem = CartManager.cartList[position]
            CartManager.cartList.removeAt(position)
            adapter.notifyDataSetChanged()
            updateTotals()
            Toast.makeText(this, "Removed ${removedItem.name}", Toast.LENGTH_SHORT).show()
            true
        }

        // 5. Checkout
        findViewById<Button>(R.id.btnCheckout).setOnClickListener {
            if (CartManager.cartList.isEmpty()) {
                Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CheckoutActivity::class.java).apply {
                    putParcelableArrayListExtra("SELECTED_PRODUCTS", ArrayList(CartManager.cartList))
                }
                startActivity(intent)
            }
        }
    }

    private fun updateTotals() {
        val totalAmount = CartManager.cartList.sumOf { it.price }
        val itemCount   = CartManager.cartList.size
        tvTotal.text    = "₱ ${"%.2f".format(totalAmount)}"
        tvSubtotal.text = "$itemCount Items"
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateTotals()
    }
}
