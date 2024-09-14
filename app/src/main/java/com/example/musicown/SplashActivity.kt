package com.example.musicown

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // Splash screen delay time (2 seconds)
    private val splashScreenDuration: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Handler to delay starting the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish() // Close splash activity
        }, splashScreenDuration)
    }
}
