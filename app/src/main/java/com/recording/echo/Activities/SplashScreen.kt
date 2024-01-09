package com.recording.echo.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.recording.echo.MainActivity
import com.recording.echo.databinding.SplashScreenBinding

class SplashScreen : AppCompatActivity() {
    val TAG = "Splash Screen"
    private lateinit var binding: SplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "In splash screen")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()}, 2000)

    }
}