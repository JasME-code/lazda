package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProductDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityproductdetail)

        val product = intent.getParcelableExtra<Product>("PRODUCT_DATA")

        if (product == null) {
            Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Basic info
        findViewById<TextView>(R.id.tvProductName).text   = product.name
        findViewById<TextView>(R.id.tvProductPrice).text  = "₱ ${"%.2f".format(product.price)}"
        findViewById<TextView>(R.id.tvProductRating).text = "⭐ ${product.rating} / 5.0"
        findViewById<TextView>(R.id.tvProductSeller).text = "Sold by: ${product.seller}"
        findViewById<ImageView>(R.id.ivProductImage).setImageResource(product.imageRes)

        // Stock status
        val tvStock = findViewById<TextView>(R.id.tvStockStatus)
        when {
            product.stock <= 0 -> {
                tvStock.text = "Out of Stock"
                tvStock.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                findViewById<Button>(R.id.btnAddToCart).isEnabled = false
                findViewById<Button>(R.id.btnBuyNow).isEnabled    = false
            }
            product.stock <= 5 -> {
                tvStock.text = "Only ${product.stock} left!"
                tvStock.setTextColor(android.graphics.Color.parseColor("#FF6900"))
            }
            else -> {
                tvStock.text = "In Stock (${product.stock} available)"
                tvStock.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            }
        }

        // Specs
        val specsBuilder = StringBuilder()
        if (product.material.isNotBlank()) specsBuilder.appendLine("• Material: ${product.material}")
        if (product.usage.isNotBlank())    specsBuilder.appendLine("• Usage: ${product.usage}")
        if (product.category.isNotBlank()) specsBuilder.appendLine("• Category: ${product.category}")
        product.details.forEach { detail ->
            if (detail.isNotBlank()) specsBuilder.appendLine("• $detail")
        }
        val specsText = specsBuilder.toString().trim()
        findViewById<TextView>(R.id.tvProductSpecs).text =
            if (specsText.isNotEmpty()) specsText else "No specifications available."

        // Reviews
        val llReviews = findViewById<LinearLayout>(R.id.llReviewsContainer)
        llReviews.removeAllViews()
        val reviewSummary = TextView(this).apply {
            text      = "⭐ ${product.rating} / 5.0 average rating\nBe the first to review this product!"
            textSize  = 14f
            setTextColor(android.graphics.Color.parseColor("#616161"))
        }
        llReviews.addView(reviewSummary)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // ✅ FIXED: Use addToCart() instead of cartList.add()
        // cartList is now a read-only computed property — cannot call .add() on it directly
        findViewById<Button>(R.id.btnAddToCart).setOnClickListener {
            if (CartManager.isInCart(product)) {
                Toast.makeText(this, "${product.name} is already in your cart!", Toast.LENGTH_SHORT).show()
            } else {
                CartManager.addToCart(this, product)
                Toast.makeText(this, "${product.name} added to cart! 🛒", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        // Buy now — go straight to checkout
        findViewById<Button>(R.id.btnBuyNow).setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putParcelableArrayListExtra("SELECTED_PRODUCTS", arrayListOf(product))
            }
            startActivity(intent)
        }
    }
}