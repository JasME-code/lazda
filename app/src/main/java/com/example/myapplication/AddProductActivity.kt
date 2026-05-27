package com.example.myapplication

// --- FIREBASE IMPORTS ---
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

        val btnSubmit    = findViewById<Button>(R.id.btnSubmitProduct)
        val etName       = findViewById<EditText>(R.id.etProductName)
        val etPrice      = findViewById<EditText>(R.id.etProductPrice)
        val etQuantity   = findViewById<EditText>(R.id.etProductQuantity)
        val etDesc       = findViewById<EditText>(R.id.etProductDesc)
        val btnBack      = findViewById<ImageButton>(R.id.btnBack)

        btnBack?.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val name     = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val qtyStr   = etQuantity.text.toString().trim()
            val desc     = etDesc.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price    = priceStr.toDoubleOrNull() ?: 0.0
            val quantity = qtyStr.toIntOrNull() ?: 0

            // FIX #7: Get actual seller name from Firebase instead of hardcoding "My Local Shop"
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseDatabase.getInstance().getReference("users").child(uid)
                .get().addOnSuccessListener { snapshot ->
                    val sellerName = snapshot.child("name").getValue(String::class.java) ?: "Unknown Seller"

                    // FIX #6: Use Firebase push key instead of timestamp.toInt() to avoid ID collisions
                    val dbRef  = FirebaseDatabase.getInstance().getReference("products")
                    val newKey = dbRef.push().key ?: uid + "_" + System.currentTimeMillis()

                    val newProduct = Product(
                        id       = newKey.hashCode(),
                        name     = name,
                        price    = price,
                        rating   = 5.0f,
                        seller   = sellerName,
                        imageRes = R.drawable.img,
                        category = "General",
                        stock    = quantity,
                        material = "Standard",
                        usage    = "General",
                        details  = listOf(desc),
                        isRestricted = false,
                        sellerUid    = uid,
                        firebaseKey  = newKey
                    )

                    dbRef.child(newKey).setValue(newProduct)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Success: $name is now live!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Could not fetch seller info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
