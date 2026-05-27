package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpassword)

        auth = FirebaseAuth.getInstance()

        val btnBack      = findViewById<ImageView>(R.id.btnBack)
        val btnSendCode  = findViewById<Button>(R.id.btnSendCode)
        val etResetEmail = findViewById<TextInputEditText>(R.id.etResetEmail)
        val tvSupport    = findViewById<TextView>(R.id.tvSupport)

        btnBack.setOnClickListener { finish() }

        btnSendCode.setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            when {
                email.isEmpty() -> {
                    Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Reset email sent to $email. Check your inbox! 📧",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }

        tvSupport.setOnClickListener {
            Toast.makeText(this, "Contacting Customer Support... 📞", Toast.LENGTH_SHORT).show()
        }
    }
}