package com.samyak.urlplayer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.samyak.urlplayer.databinding.ActivityMainBinding
import com.samyak.urlplayer.AppUpdate.InAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.samyak.urlplayer.AdManage.loadBannerAd
import com.samyak.urlplayer.utils.AppConstants
import com.samyak.urlplayer.utils.LanguageManager
import java.util.Locale
import android.content.Context
import com.samyak.urlplayer.screen.AboutFragment
import com.samyak.urlplayer.screen.HomeFragment
import com.samyak.urlplayer.screen.URLActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    // Add install state listener for app updates
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Show a snackbar or notification that the update has been downloaded
            Toast.makeText(
                this,
                "Update downloaded. Restart to install.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Register the launcher for app updates
    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.d(TAG, "Update flow failed! Result code: ${result.resultCode}")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-detect system language on first run
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.contains("language_set")) {
            // Get system language
            val systemLang = Locale.getDefault().language
            
            // Check if we support this language
            val supportedLanguages = LanguageManager.getSupportedLanguages()
            val isSupported = supportedLanguages.any { it.second == systemLang }
            
            if (isSupported) {
                // Set app language to system language
                prefs.edit()
                    .putString("language", systemLang)
                    .putBoolean("language_set", true)
                    .apply()
                
                // Recreate to apply language
                recreate()
            } else {
                // Mark as set but keep default (English)
                prefs.edit()
                    .putBoolean("language_set", true)
                    .apply()
            }
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize In-App Update Manager
        InAppUpdateManager.init(this, updateResultLauncher)
        InAppUpdateManager.registerListener(installStateUpdatedListener)

//        binding.bannerAdContainer.loadBannerAd()
        setupToolbar()
        setupClickListeners()
        setupNavigationDrawer()

        bottomNavigation()

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.playlist-> setFragment(HomeFragment())

                R.id.profile-> setFragment(AboutFragment())

            }
            return@setOnItemSelectedListener true

        }


        // Setup FAB
        binding.addUrl.setOnClickListener {
            startActivity(Intent(this, URLActivity::class.java))
        }
    }

    private fun bottomNavigation() {
        // Set the default fragment when app starts
        setFragment(HomeFragment())
    }

    private fun setFragment(fragment: androidx.fragment.app.Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_container, fragment)
        transaction.commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
            title = getString(R.string.app_name)
        }
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun setupClickListeners() {
//        binding.openHome.setOnClickListener {
//            startActivity(Intent(this, HomeActivity::class.java))
//        }
    }

    private fun setupNavigationDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_rate -> {
                    rateApp()
                    true
                }
                R.id.nav_share -> {
                    shareApp()
                    true
                }
                R.id.nav_privacy -> {
                    openPrivacyPolicy()
                    true
                }
                R.id.nav_contact -> {
                    contactUs()
                    true
                }

                else -> false
            }
        }
    }

    private fun rateApp() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("${AppConstants.MARKET_BASE_URL}$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("${AppConstants.PLAY_STORE_BASE_URL}$packageName")))
        }
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,
                "Watch your favorite videos with ${getString(R.string.app_name)}!\n" +
                        "Download now: ${AppConstants.PLAY_STORE_BASE_URL}$packageName")
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_message)))
    }

    private fun openPrivacyPolicy() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PRIVACY_POLICY_URL)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_privacy_policy), Toast.LENGTH_SHORT).show()
        }
    }

    private fun contactUs() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${AppConstants.CONTACT_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.contact_us)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_no_email), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister the update listener
        InAppUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}