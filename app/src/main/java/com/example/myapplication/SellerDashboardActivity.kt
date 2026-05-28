package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SellerDashboardActivity : AppCompatActivity() {

    private lateinit var tvRevenue: TextView
    private lateinit var tvCount: TextView
    private lateinit var lvOrders: ListView

    private val liveOrders = mutableListOf<Map<String, Any>>()
    private lateinit var orderAdapter: ArrayAdapter<String>
    private var ordersListener: ValueEventListener? = null

    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_dashboard)

        tvRevenue     = findViewById(R.id.tvTotalRevenue)
        tvCount       = findViewById(R.id.tvTotalItemsSold)
        lvOrders      = findViewById(R.id.lvOrdersToFulfill)
        val btnAdd    = findViewById<Button>(R.id.btnAddProduct)
        val btnEdit   = findViewById<Button>(R.id.btnViewListings)
        val btnLogout = findViewById<ImageView>(R.id.btnLogout)

        // ✅ FIXED: Custom adapter with dark text — simple_list_item_1 shows white/invisible text
        orderAdapter = object : ArrayAdapter<String>(this, R.layout.item_list_text, R.id.tvLine1, mutableListOf()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_list_text, parent, false)
                val text  = getItem(position) ?: ""
                val lines = text.split("\n")
                view.findViewById<TextView>(R.id.tvLine1).text = lines.getOrNull(0) ?: ""
                view.findViewById<TextView>(R.id.tvLine2).text = lines.drop(1).joinToString("\n")
                return view
            }
        }
        lvOrders.adapter = orderAdapter

        listenToSellerOrders()

        lvOrders.setOnItemClickListener { _, _, position, _ ->
            if (position >= liveOrders.size) return@setOnItemClickListener
            val orderMap  = liveOrders[position]
            val orderId   = orderMap["orderId"] as? String ?: return@setOnItemClickListener
            val status    = orderMap["status"] as? String ?: "Pending"
            val newStatus = when (status) { "Pending" -> "Shipped"; "Shipped" -> "Delivered"; else -> null }
            if (newStatus == null) {
                Toast.makeText(this, "Order already finalized.", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            FirebaseDatabase.getInstance(databaseUrl).getReference("orders").child(orderId).child("status")
                .setValue(newStatus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Order $newStatus! ${if (newStatus == "Shipped") "🚚" else "💰"}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnAdd.setOnClickListener  { startActivity(Intent(this, AddProductActivity::class.java)) }
        btnEdit.setOnClickListener { startActivity(Intent(this, EditListingsActivity::class.java)) }
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout").setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent); finish()
                }.setNegativeButton("Cancel", null).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ordersListener?.let {
            FirebaseDatabase.getInstance(databaseUrl).getReference("orders").removeEventListener(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun listenToSellerOrders() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(currentUid)
            .get().addOnSuccessListener { userSnapshot ->
                val sellerName = userSnapshot.child("name").getValue(String::class.java) ?: ""

                ordersListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        liveOrders.clear()
                        var totalRevenue   = 0.0
                        var deliveredCount = 0

                        for (child in snapshot.children) {
                            val orderMap = child.value as? Map<String, Any> ?: continue
                            val items = orderMap["items"] as? List<Map<String, Any>> ?: emptyList()
                            val myItems = items.filter { item ->
                                item["seller"] == sellerName || item["sellerUid"] == currentUid
                            }
                            if (myItems.isEmpty()) continue
                            liveOrders.add(orderMap)
                            if (orderMap["status"] == "Delivered") {
                                myItems.forEach { item ->
                                    totalRevenue += (item["price"] as? Number)?.toDouble() ?: 0.0
                                }
                                deliveredCount += myItems.size
                            }
                        }

                        tvRevenue.text = "₱ ${"%.2f".format(totalRevenue)}"
                        tvCount.text   = "Items Sold: $deliveredCount"

                        val orderStrings = liveOrders.mapIndexed { i, o ->
                            val status = o["status"] as? String ?: "Pending"
                            val emoji  = when (status) { "Pending" -> "⏳"; "Shipped" -> "🚚"; else -> "✅" }
                            "Order #${i+1}  $emoji $status\n₱${o["totalPrice"]}  •  ${o["date"] ?: ""}"
                        }
                        orderAdapter.clear()
                        orderAdapter.addAll(orderStrings)
                        orderAdapter.notifyDataSetChanged()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SellerDashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                FirebaseDatabase.getInstance(databaseUrl).getReference("orders")
                    .addValueEventListener(ordersListener!!)
            }
    }
}