package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditListingsActivity : AppCompatActivity() {

    private val sellerProducts = mutableListOf<Product>()
    private lateinit var adapter: Dashboard.ProductAdapter
    private var listingsListener: ValueEventListener? = null

    // ✅ FIXED: Correct database URL matching google-services.json
    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_listings)

        val lvEditListings = findViewById<ListView>(R.id.lvEditListings)
        val btnBack        = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        adapter = Dashboard.ProductAdapter(this, sellerProducts)
        lvEditListings.adapter = adapter

        fetchSellerProducts()

        lvEditListings.setOnItemClickListener { _, _, position, _ ->
            val product = sellerProducts[position]
            startActivity(Intent(this, UpdateProductActivity::class.java).apply {
                putExtra("PRODUCT_DATA", product)
            })
        }
    }

    private fun fetchSellerProducts() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // ✅ FIXED: Uses databaseUrl
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("products")
        listingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                sellerProducts.clear()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    if (product != null && product.sellerUid == currentUid) sellerProducts.add(product)
                }
                adapter.notifyDataSetChanged()
                if (sellerProducts.isEmpty()) Toast.makeText(this@EditListingsActivity, "No listings yet. Add a product first!", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditListingsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        dbRef.addValueEventListener(listingsListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("products")
        listingsListener?.let { dbRef.removeEventListener(it) }
    }
}