package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private var calculatedAge: Int = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressRegister: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        auth             = FirebaseAuth.getInstance()
        btnRegister      = findViewById(R.id.btnRegister)
        progressRegister = findViewById(R.id.progressRegister)

        val etName       = findViewById<TextInputEditText>(R.id.etName)
        val etEmail      = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword   = findViewById<TextInputEditText>(R.id.etPassword)
        val etDOB        = findViewById<TextInputEditText>(R.id.etDOB)
        val switchAge    = findViewById<SwitchMaterial>(R.id.switchAgeConfirmation)
        val btnBack      = findViewById<ImageButton>(R.id.btnBack)
        val tvAgeDisplay = findViewById<TextView>(R.id.tvAge)
        val rbSeller     = findViewById<RadioButton>(R.id.rbSeller)

        btnBack.setOnClickListener { finish() }

        etDOB.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val birth = Calendar.getInstance().apply { set(y, m, d) }
                etDOB.setText(SimpleDateFormat("MM/dd/yyyy", Locale.US).format(birth.time))
                val today = Calendar.getInstance()
                calculatedAge = today.get(Calendar.YEAR) - y
                if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) calculatedAge--
                tvAgeDisplay.text = "Age: $calculatedAge"
            }, cal.get(Calendar.YEAR) - 20, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnRegister.setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role     = if (rbSeller.isChecked) "Seller" else "Customer"

            when {
                name.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !switchAge.isChecked -> {
                    Toast.makeText(this, "Please confirm age status", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            setRegisterLoading(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: run {
                        setRegisterLoading(false)
                        return@addOnSuccessListener
                    }
                    val userProfile = hashMapOf(
                        "uid"   to uid,
                        "name"  to name,
                        "email" to email,
                        "age"   to calculatedAge,
                        "role"  to role
                    )
                    FirebaseDatabase.getInstance().getReference("users").child(uid)
                        .setValue(userProfile)
                        .addOnSuccessListener {
                            setRegisterLoading(false)
                            Toast.makeText(this, "Account created! Please log in. ✅", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            setRegisterLoading(false)
                            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    setRegisterLoading(false)
                    Toast.makeText(this, "Auth Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setRegisterLoading(loading: Boolean) {
        btnRegister.isEnabled       = !loading
        btnRegister.text            = if (loading) "" else "CREATE ACCOUNT"
        progressRegister.visibility = if (loading) View.VISIBLE else View.GONE
    }
}