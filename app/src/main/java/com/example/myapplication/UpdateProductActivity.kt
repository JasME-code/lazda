package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UpdateProductActivity : AppCompatActivity() {

    // ✅ FIXED: Correct database URL matching google-services.json
    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_product)

        val product = intent.getParcelableExtra<Product>("PRODUCT_DATA")
        if (product == null) {
            Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etName    = findViewById<EditText>(R.id.etUpdateName)
        val etPrice   = findViewById<EditText>(R.id.etUpdatePrice)
        val etDesc    = findViewById<EditText>(R.id.etUpdateDesc)
        val btnSave   = findViewById<Button>(R.id.btnUpdateProduct)
        val btnDelete = findViewById<Button>(R.id.btnDeleteProduct)

        etName.setText(product.name)
        etPrice.setText(product.price.toString())
        etDesc.setText(product.details.firstOrNull() ?: "")

        // ✅ FIXED: Uses databaseUrl
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("products")
        val key   = product.firebaseKey.ifEmpty { product.id.toString() }

        btnSave.setOnClickListener {
            val newName  = etName.text.toString().trim()
            val newPrice = etPrice.text.toString().trim().toDoubleOrNull()
            val newDesc  = etDesc.text.toString().trim()

            if (newName.isEmpty() || newPrice == null) {
                Toast.makeText(this, "Please fill in name and price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf<String, Any>(
                "name"    to newName,
                "price"   to newPrice,
                "details" to listOf(newDesc)
            )

            dbRef.child(key).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete listing")
                .setMessage("Are you sure you want to delete \"${product.name}\"? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    dbRef.child(key).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}