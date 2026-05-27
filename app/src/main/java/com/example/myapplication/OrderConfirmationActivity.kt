package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OrderConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        findViewById<Button>(R.id.btnContinueShopping).setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnViewOrders).setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }
    }

    // Prevent going back to checkout after order placed
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Dashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
