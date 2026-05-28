package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitycheckout)

        val selectedProducts = intent.getParcelableArrayListExtra<Product>("SELECTED_PRODUCTS") ?: arrayListOf()

        val lvCheckoutItems = findViewById<ListView>(R.id.lvCheckoutItems)
        val tvTotalAmount   = findViewById<TextView>(R.id.tvOrderTotal)
        val tvItemQty       = findViewById<TextView>(R.id.tvOrderQty)
        val btnPlaceOrder   = findViewById<Button>(R.id.btnPlaceOrder)
        val progressOrder   = findViewById<ProgressBar>(R.id.progressOrder)
        val btnBack         = findViewById<ImageView>(R.id.btnBack)
        val etAddress       = findViewById<EditText>(R.id.etAddress)
        val spinnerPayment  = findViewById<Spinner>(R.id.spinnerPayment)

        spinnerPayment.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Cash on Delivery (COD)", "GCash", "Lazada Wallet", "Credit Card")
        )

        lvCheckoutItems.adapter = Dashboard.ProductAdapter(this, selectedProducts)

        val totalAmount    = selectedProducts.sumOf { it.price }
        val itemCount      = selectedProducts.size
        tvTotalAmount.text = "₱ ${"%.2f".format(totalAmount)}"
        tvItemQty.text     = "Total ($itemCount items)"

        btnBack.setOnClickListener { finish() }

        btnPlaceOrder.setOnClickListener {
            val address       = etAddress.text.toString().trim()
            val paymentMethod = spinnerPayment.selectedItem.toString()

            if (selectedProducts.isEmpty()) {
                Toast.makeText(this, "No items to order!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter your shipping address!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to place an order.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPlaceOrder.isEnabled  = false
            btnPlaceOrder.text       = ""
            progressOrder.visibility = View.VISIBLE

            val db        = FirebaseDatabase.getInstance(databaseUrl)
            val ordersRef = db.getReference("orders")
            val orderId   = ordersRef.push().key ?: "ORD-${System.currentTimeMillis()}"
            val sdf       = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

            val itemsSummary = selectedProducts.map { p ->
                mapOf(
                    "id"          to p.id,
                    "name"        to p.name,
                    "price"       to p.price,
                    "seller"      to p.seller,
                    "sellerUid"   to p.sellerUid,
                    "firebaseKey" to p.firebaseKey
                )
            }

            val orderMap = hashMapOf(
                "orderId"       to orderId,
                "buyerUid"      to currentUser.uid,
                "totalPrice"    to totalAmount,
                "address"       to address,
                "paymentMethod" to paymentMethod,
                "date"          to sdf.format(Date()),
                "status"        to "Pending",
                "itemCount"     to itemCount,
                "items"         to itemsSummary
            )

            ordersRef.child(orderId).setValue(orderMap)
                .addOnSuccessListener {
                    val productsRef = db.getReference("products")
                    var pending = selectedProducts.size

                    for (product in selectedProducts) {
                        val key = product.firebaseKey.ifEmpty { product.id.toString() }
                        if (key.isBlank()) {
                            pending--
                            if (pending == 0) finishOrder()
                            continue
                        }
                        productsRef.child(key).child("stock")
                            .runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    val stock = currentData.getValue(Int::class.java) ?: 0
                                    currentData.value = maxOf(0, stock - 1)
                                    return Transaction.success(currentData)
                                }
                                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                    pending--
                                    if (pending == 0) finishOrder()
                                }
                            })
                    }
                    if (selectedProducts.isEmpty()) finishOrder()
                }
                .addOnFailureListener { e ->
                    btnPlaceOrder.isEnabled  = true
                    btnPlaceOrder.text       = "PLACE ORDER"
                    progressOrder.visibility = View.GONE
                    Toast.makeText(this, "Failed to place order: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun finishOrder() {
        // ✅ FIXED: Use clearCart(context) instead of cartList.clear()
        // cartList is now a computed read-only property in CartManager — it cannot be cleared directly
        CartManager.clearCart(this)

        Toast.makeText(this, "Order Placed Successfully! 🎉", Toast.LENGTH_LONG).show()
        val intent = Intent(this, OrderConfirmationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}