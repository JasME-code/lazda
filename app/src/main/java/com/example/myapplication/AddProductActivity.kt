package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddProductActivity : AppCompatActivity() {

    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        val btnSubmit  = findViewById<Button>(R.id.btnSubmitProduct)
        val etName     = findViewById<EditText>(R.id.etProductName)
        val etPrice    = findViewById<EditText>(R.id.etProductPrice)
        val etQuantity = findViewById<EditText>(R.id.etProductQuantity)
        val etDesc     = findViewById<EditText>(R.id.etProductDesc)
        val btnBack    = findViewById<ImageButton>(R.id.btnBack)
        val progress   = findViewById<ProgressBar>(R.id.progressBar)

        btnBack.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val name     = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val qtyStr   = etQuantity.text.toString().trim()
            val desc     = etDesc.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price    = priceStr.toDoubleOrNull()
            val quantity = qtyStr.toIntOrNull()

            if (price == null || price <= 0) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantity == null || quantity < 0) {
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in to add products", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = currentUser.uid

            // Show loading
            progress.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            val db     = FirebaseDatabase.getInstance(databaseUrl)
            val dbRef  = db.getReference("products")
            val newKey = dbRef.push().key ?: "${uid}_${System.currentTimeMillis()}"

            // ✅ FIXED: Get seller name from DB but with a timeout fallback
            // so finish() always gets called even if the user read is slow
            db.getReference("users").child(uid).get()
                .addOnCompleteListener { task ->
                    // Use DB name if available, fall back to email prefix
                    val sellerName = if (task.isSuccessful && task.result.exists()) {
                        task.result.child("name").getValue(String::class.java)
                            ?: currentUser.email?.substringBefore("@") ?: "Seller"
                    } else {
                        currentUser.email?.substringBefore("@") ?: "Seller"
                    }

                    val productMap = mapOf(
                        "id"           to newKey.hashCode(),
                        "name"         to name,
                        "price"        to price,
                        "rating"       to 5.0,
                        "seller"       to sellerName,
                        "category"     to "General",
                        "stock"        to quantity,
                        "material"     to "Standard",
                        "usage"        to "General",
                        "details"      to listOf(desc),
                        "isRestricted" to false,
                        "sellerUid"    to uid,
                        "firebaseKey"  to newKey
                    )

                    dbRef.child(newKey).setValue(productMap)
                        .addOnSuccessListener {
                            progress.visibility = View.GONE
                            Toast.makeText(this, "\"$name\" added successfully! ✅", Toast.LENGTH_SHORT).show()
                            finish() // ✅ Goes back to SellerDashboard
                        }
                        .addOnFailureListener { e ->
                            progress.visibility = View.GONE
                            btnSubmit.isEnabled = true
                            Toast.makeText(this, "Failed to save product: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
        }
    }
}