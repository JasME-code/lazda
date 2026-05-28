package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OrderHistoryActivity : AppCompatActivity() {

    private val orders = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var ordersListener: ValueEventListener? = null

    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        val lvOrders = findViewById<ListView>(R.id.lvOrderHistory)
        val btnBack  = findViewById<ImageView>(R.id.btnBack)
        val tvEmpty  = findViewById<TextView>(R.id.tvEmptyOrders)
        btnBack.setOnClickListener { finish() }

        // ✅ FIXED: Custom adapter with dark text — simple_list_item_1 shows white/invisible text
        adapter = object : ArrayAdapter<String>(this, R.layout.item_list_text, R.id.tvLine1, orders) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_list_text, parent, false)
                val text = getItem(position) ?: ""
                val lines = text.split("\n")
                view.findViewById<TextView>(R.id.tvLine1).text = lines.getOrNull(0) ?: ""
                view.findViewById<TextView>(R.id.tvLine2).text =
                    lines.drop(1).joinToString("\n")
                return view
            }
        }
        lvOrders.adapter = adapter

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(this, "Please log in to view orders", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        var orderNumber = 0

        ordersListener = object : ValueEventListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(snapshot: DataSnapshot) {
                orders.clear()
                orderNumber = 0
                for (child in snapshot.children) {
                    val orderMap = child.value as? Map<String, Any> ?: continue
                    if (orderMap["buyerUid"] != currentUid) continue

                    orderNumber++
                    val status  = orderMap["status"] as? String ?: "Pending"
                    val total   = (orderMap["totalPrice"] as? Number)?.toDouble() ?: 0.0
                    val date    = orderMap["date"] as? String ?: ""
                    val address = orderMap["address"] as? String ?: ""
                    val payment = orderMap["paymentMethod"] as? String ?: ""

                    val statusEmoji = when (status) {
                        "Pending"   -> "⏳"
                        "Shipped"   -> "🚚"
                        "Delivered" -> "✅"
                        else        -> "📦"
                    }
                    orders.add(
                        "Order #$orderNumber  $statusEmoji $status\n" +
                                "Total: ₱${"%.2f".format(total)}  •  $date\n" +
                                "📍 $address\n💳 $payment"
                    )
                }
                adapter.notifyDataSetChanged()
                tvEmpty.visibility  = if (orders.isEmpty()) View.VISIBLE else View.GONE
                lvOrders.visibility = if (orders.isEmpty()) View.GONE    else View.VISIBLE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderHistoryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        // ✅ Also fixed: uses databaseUrl
        FirebaseDatabase.getInstance(databaseUrl).getReference("orders")
            .addValueEventListener(ordersListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.let {
            FirebaseDatabase.getInstance(databaseUrl).getReference("orders").removeEventListener(it)
        }
    }
}