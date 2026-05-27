package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddProductActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // UI Element Bindings
        val btnSubmit    = findViewById<Button>(R.id.btnSubmitProduct)
        val etName       = findViewById<EditText>(R.id.etProductName)
        val etPrice      = findViewById<EditText>(R.id.etProductPrice)
        val etQuantity   = findViewById<EditText>(R.id.etProductQuantity)
        val etDesc       = findViewById<EditText>(R.id.etProductDesc)
        val btnBack      = findViewById<ImageButton>(R.id.btnBack)

        // Back button action
        btnBack?.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val name     = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val qtyStr   = etQuantity.text.toString().trim()
            val desc     = etDesc.text.toString().trim()

            // 1. Inputs Validation Guard
            if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price    = priceStr.toDoubleOrNull() ?: 0.0
            val quantity = qtyStr.toIntOrNull() ?: 0

            // 2. Authentication Guard
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Error: You must be logged in to add products.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Define Region Database URL
            val databaseUrl = "https://lazada-5cf23-default-rtdb.asia-southeast1.firebasedatabase.app/"

            // 4. Fetch Seller Display Name
            FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(uid)
                .get().addOnSuccessListener { snapshot ->

                    val sellerName = snapshot.child("name").getValue(String::class.java) ?: "Unknown Seller"

                    // 5. Generate Target Key Node
                    val dbRef  = FirebaseDatabase.getInstance(databaseUrl).getReference("products")
                    val newKey = dbRef.push().key ?: (uid + "_" + System.currentTimeMillis())

                    // 6. Build the Domain Instance
                    val newProduct = Product(
                        id           = newKey.hashCode(),
                        name         = name,
                        price        = price,
                        rating       = 5.0f,
                        seller       = sellerName,
                        imageRes     = R.drawable.img,
                        category     = "General",
                        stock        = quantity,
                        material     = "Standard",
                        usage        = "General",
                        details      = listOf(desc),
                        isRestricted = false,
                        sellerUid    = uid,
                        firebaseKey  = newKey
                    )

                    // 7. Write Structured MAP Payload to database node
                    dbRef.child(newKey).setValue(newProduct.toFirebaseMap())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Success: $name is now live!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Database Write Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Profile Fetch Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}