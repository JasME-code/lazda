package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

val database = FirebaseDatabase.getInstance().getReference("products")

class Dashboard : AppCompatActivity() {

    private val masterList   = mutableListOf<Product>()
    private val displayList  = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter
    private var userAge: Int = 0

    // IMPROVEMENT #5: Remember the last search query
    private var lastQuery: String = ""

    // IMPROVEMENT #9: Hold reference to listener so we can remove it in onDestroy
    private var productsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        val user = intent.getParcelableExtra<User>("USER_DATA")
        userAge  = user?.age ?: 0
        val role = user?.role ?: "Customer"

        if (user == null) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseDatabase.getInstance().getReference("users").child(uid)
                    .get().addOnSuccessListener { snapshot ->
                        userAge = snapshot.child("age").getValue(Int::class.java) ?: 0
                        filterAndRefreshList()
                    }
            }
        }

        setupProductList()
        setupClickListeners(role)
        setupSearch()
        syncWithFirebase()
    }

    private fun syncWithFirebase() {
        productsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                masterList.clear()
                if (!snapshot.exists()) {
                    uploadDefaultsToFirebase()
                } else {
                    for (item in snapshot.children) {
                        val p = item.getValue(Product::class.java)
                        if (p != null) masterList.add(p)
                    }
                    filterAndRefreshList()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Dashboard, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(productsListener!!)
    }

    // IMPROVEMENT #9: Remove listener when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        productsListener?.let { database.removeEventListener(it) }
    }

    private fun uploadDefaultsToFirebase() {
        val seedData = listOf(
            Triple("Nike Pegasus 42",    5999.0 to 15, false),
            Triple("Gaming Laptop",      45000.0 to 5, false),
            Triple("Special Item (18+)", 1200.0 to 2,  true)
        )
        val sellers = listOf("Nike Official", "TechShop", "AdultStore")
        seedData.forEachIndexed { i, (name, priceStock, restricted) ->
            val key = database.push().key ?: return@forEachIndexed
            database.child(key).setValue(Product(
                id = key.hashCode(), name = name, price = priceStock.first,
                rating = 4.9f - (i * 0.3f), seller = sellers[i], imageRes = R.drawable.img,
                category = "General", stock = priceStock.second, material = "Standard",
                usage = "General", details = listOf("Seed product"), isRestricted = restricted,
                sellerUid = "system", firebaseKey = key
            ))
        }
    }

    private fun setupProductList() {
        val lvFlash = findViewById<ListView>(R.id.listViewFlashSale)
        productAdapter = ProductAdapter(this, displayList)
        lvFlash.adapter = productAdapter
        lvFlash.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("PRODUCT_DATA", displayList[position])
            startActivity(intent)
        }
    }

    private fun filterAndRefreshList() {
        val base = if (userAge < 18) masterList.filter { !it.isRestricted } else masterList.toList()
        val filtered = if (lastQuery.isBlank()) base
                       else base.filter { it.name.lowercase().contains(lastQuery) }
        displayList.clear()
        displayList.addAll(filtered)
        productAdapter.notifyDataSetChanged()
        updateCartBadge()
    }

    // IMPROVEMENT #6: Update cart badge count
    private fun updateCartBadge() {
        val badge = findViewById<TextView>(R.id.tvCartBadge)
        val count = CartManager.cartList.size
        if (count > 0) {
            badge?.visibility = View.VISIBLE
            badge?.text       = if (count > 99) "99+" else count.toString()
        } else {
            badge?.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

    private fun setupClickListeners(role: String) {
        // IMPROVEMENT #11: Sort + category chips
        findViewById<Button>(R.id.btnSortPrice)?.setOnClickListener {
            displayList.sortBy { it.price }
            productAdapter.notifyDataSetChanged()
        }

        val btnSeller = findViewById<Button>(R.id.btnSellerCenter)
        if (role == "Seller") {
            btnSeller?.visibility = View.VISIBLE
            btnSeller?.setOnClickListener { startActivity(Intent(this, SellerDashboardActivity::class.java)) }
        }

        findViewById<View>(R.id.btnCartIcon)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout").setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent); finish()
                }.setNegativeButton("Cancel", null).show()
        }

        findViewById<View>(R.id.btnOrderHistory)?.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        findViewById<View>(R.id.btnVoucher)?.setOnClickListener {
            Toast.makeText(this, "No vouchers available right now.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        val et = findViewById<TextInputEditText>(R.id.etSearchFull)
        // IMPROVEMENT #5: Restore last query on screen resume
        et?.setText(lastQuery)

        findViewById<View>(R.id.btnSearchFull)?.setOnClickListener {
            lastQuery = et?.text.toString().trim().lowercase()
            filterAndRefreshList()
        }
    }

    class ProductAdapter(context: Context, private val products: List<Product>) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)
        override fun getCount()             = products.size
        override fun getItem(p: Int)        = products[p]
        override fun getItemId(p: Int)      = p.toLong()
        override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
            val view = v ?: inflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            val item = getItem(p)
            view.findViewById<TextView>(android.R.id.text1).text = item.name
            view.findViewById<TextView>(android.R.id.text2).text = "₱${"%.2f".format(item.price)} • Stock: ${item.stock}"
            return view
        }
    }
}
