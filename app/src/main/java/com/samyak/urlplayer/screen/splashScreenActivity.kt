package com.samyak.urlplayer.screen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.samyak.urlplayer.MainActivity
import com.samyak.urlplayer.databinding.ActivitySplashScreenBinding

class splashScreenActivity : AppCompatActivity() {

    private lateinit var  binding : ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar if needed
        supportActionBar?.hide()

        openMainActivityScreen()

    }

    private fun openMainActivityScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000) // 5 seconds delay
    }
}