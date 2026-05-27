package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnLogin: Button
    private lateinit var progressLogin: ProgressBar

    // ✅ FIXED: Correct database URL matching google-services.json
    private val databaseUrl = "https://lazada-e7c5b-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        auth.signOut() // Optional: clears past session on open

        btnLogin      = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)

        val forgotPassword = findViewById<TextView>(R.id.forgotPasswordLink)
        val registerLink   = findViewById<TextView>(R.id.registerLink)
        val etEmail        = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword     = findViewById<TextInputEditText>(R.id.etPassword)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            setLoginLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid != null) {
                        checkUserRole(uid)
                    } else {
                        setLoginLoading(false)
                        Toast.makeText(this, "Login failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    setLoginLoading(false)
                    Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setLoginLoading(loading: Boolean) {
        btnLogin.isEnabled       = !loading
        btnLogin.text            = if (loading) "" else "LOGIN"
        progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun checkUserRole(uid: String) {
        // ✅ FIXED: Uses correct databaseUrl so user record is found
        FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val role   = snapshot.child("role").getValue(String::class.java)
                    val intent = if (role == "Seller") {
                        Intent(this, SellerDashboardActivity::class.java)
                    } else {
                        Intent(this, Dashboard::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    setLoginLoading(false)
                    Toast.makeText(this, "User record not found. Please register.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                setLoginLoading(false)
                Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}