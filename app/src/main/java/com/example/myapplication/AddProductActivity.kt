package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddProductActivity : AppCompatActivity() {

    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        val btnSubmit            = findViewById<Button>(R.id.btnSubmitProduct)
        val etName               = findViewById<EditText>(R.id.etProductName)
        val etPrice              = findViewById<EditText>(R.id.etProductPrice)
        val etQuantity           = findViewById<EditText>(R.id.etProductQuantity)
        val etDesc               = findViewById<EditText>(R.id.etProductDesc)
        val btnBack              = findViewById<ImageButton>(R.id.btnBack)
        val progressBar          = findViewById<ProgressBar>(R.id.progressBar)
        val spinnerAge           = findViewById<Spinner>(R.id.spinnerAgeRestriction)

        // ✅ Age restriction options — seller picks who can see this product
        val ageOptions = listOf(
            "No Restriction (Everyone can see)",
            "13+ (Teen and above)",
            "18+ (Adults only)"
        )
        // Maps display label → actual minAge value
        val ageValues = mapOf(
            "No Restriction (Everyone can see)" to 0,
            "13+ (Teen and above)"              to 13,
            "18+ (Adults only)"                 to 18
        )

        spinnerAge.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ageOptions
        )

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

            // ✅ Get selected minAge from spinner
            val selectedLabel = spinnerAge.selectedItem.toString()
            val minAge        = ageValues[selectedLabel] ?: 0

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in to add products", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = currentUser.uid

            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled    = false
            btnSubmit.text         = "Uploading..."

            val db     = FirebaseDatabase.getInstance(databaseUrl)
            val dbRef  = db.getReference("products")
            val newKey = dbRef.push().key ?: "${uid}_${System.currentTimeMillis()}"

            db.getReference("users").child(uid).get()
                .addOnCompleteListener { task ->
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
                        "isRestricted" to (minAge >= 18),
                        "minAge"       to minAge,       // ✅ saved to Firebase
                        "sellerUid"    to uid,
                        "firebaseKey"  to newKey
                    )

                    dbRef.child(newKey).setValue(productMap)
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            val label = if (minAge == 0) "visible to everyone"
                            else "restricted to $minAge+ customers"
                            Toast.makeText(
                                this,
                                "\"$name\" uploaded! ($label) ✅",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnSubmit.isEnabled    = true
                            btnSubmit.text         = "UPLOAD TO SHOP"
                            Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
        }
    }
}