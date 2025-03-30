package com.samyak.urlplayer.screen

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.samyak.urlplayer.AdManage.loadBannerAd
import com.samyak.urlplayer.R
import com.samyak.urlplayer.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAboutBinding
    
    companion object {
        private const val TAG = "AboutActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bannerContainer.loadBannerAd()
        initializeUI()
        setupClickListeners()
    }

    private fun initializeUI() {
        setupToolbar()
        setupVersionInfo()
    }

    private fun setupClickListeners() {
        // Simple click listeners without ad logic
        binding.developerInfo.setOnClickListener {
            // No action needed
        }

        binding.appDescription.setOnClickListener {
            // No action needed
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.appVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting package info", e)
            binding.appVersion.text = getString(R.string.version_format, "Unknown")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.about)
            
            binding.toolbar.navigationIcon?.setTint(
                ContextCompat.getColor(this@AboutActivity, android.R.color.white)
            )
        }
        
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(this, android.R.color.white)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 