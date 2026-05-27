package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loadingscreen)

        animateLoadingDots()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // This will tell you in Logcat if the transition failed
                e.printStackTrace()
            }
        }, 3000)
    }

    private fun animateLoadingDots() {
        val dot1 = findViewById<View>(R.id.dot1) ?: return
        val dot2 = findViewById<View>(R.id.dot2) ?: return
        val dot3 = findViewById<View>(R.id.dot3) ?: return

        val animator1 = ObjectAnimator.ofFloat(dot1, "alpha", 1f, 0.4f)
        val animator2 = ObjectAnimator.ofFloat(dot2, "alpha", 0.4f, 1f, 0.4f)
        val animator3 = ObjectAnimator.ofFloat(dot3, "alpha", 0.4f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(animator1, animator2, animator3)
        animatorSet.duration = 1200

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Using a check to ensure activity is still alive before restarting animation
                if (!isFinishing) animatorSet.start()
            }
        })
        animatorSet.start()
    }
}