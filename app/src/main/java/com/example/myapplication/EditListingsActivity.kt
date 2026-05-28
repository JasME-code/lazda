package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditListingsActivity : AppCompatActivity() {

    private val sellerProducts = mutableListOf<Product>()
    private lateinit var adapter: SellerProductAdapter
    private var listingsListener: ValueEventListener? = null

    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_listings)

        val lvEditListings = findViewById<ListView>(R.id.lvEditListings)
        val btnBack        = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        adapter = SellerProductAdapter(this, sellerProducts)
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
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("products")
        listingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                sellerProducts.clear()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    if (product != null && product.sellerUid == currentUid) sellerProducts.add(product)
                }
                adapter.notifyDataSetChanged()
                if (sellerProducts.isEmpty())
                    Toast.makeText(this@EditListingsActivity, "No listings yet. Add a product first!", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditListingsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        dbRef.addValueEventListener(listingsListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        listingsListener?.let {
            FirebaseDatabase.getInstance(databaseUrl).getReference("products").removeEventListener(it)
        }
    }

    // ✅ FIXED: Custom adapter using item_list_text.xml with explicit dark text colors
    // so product name and price are always visible on white background
    class SellerProductAdapter(
        private val context: Context,
        private val products: List<Product>
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(context)

        override fun getCount()        = products.size
        override fun getItem(i: Int)   = products[i]
        override fun getItemId(i: Int) = i.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_list_text, parent, false)
            val product = getItem(position)

            // Line 1: product name — dark bold text
            view.findViewById<TextView>(R.id.tvLine1).text = product.name

            // Line 2: price + stock — visible subtitle text
            view.findViewById<TextView>(R.id.tvLine2).text =
                "₱${"%.2f".format(product.price)}  •  Stock: ${product.stock}  •  Tap to edit"

            return view
        }
    }
}