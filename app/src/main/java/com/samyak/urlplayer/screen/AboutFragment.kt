package com.samyak.urlplayer.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.samyak.urlplayer.AdManage.loadBannerAd
import com.samyak.urlplayer.R
import com.samyak.urlplayer.databinding.FragmentAboutBinding
import com.samyak.urlplayer.utils.AppConstants

class AboutFragment : Fragment() {
    
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    
    companion object {
        private const val TAG = "AboutFragment"
        
        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bannerContainer.loadBannerAd()
        initializeUI()
        setupClickListeners()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeUI() {
        setupVersionInfo()
    }

    private fun setupClickListeners() {
        // Developer info card
        binding.developerInfo.setOnClickListener {
            // No specific action needed or could open developer website
        }

        // App description card
        binding.appDescription.setOnClickListener {
            // No specific action needed
        }
        
        // Share button
        binding.btnShare.setOnClickListener {
            shareApp()
        }
        
        // Rate app button
        binding.btnRate.setOnClickListener {
            rateApp()
        }
        
        // Clear recent list button
        binding.btnClear.setOnClickListener {
            showClearHistoryConfirmation()
        }
        
        // Feedback button
        binding.btnFeedback.setOnClickListener {
            sendFeedback()
        }
        
        // Privacy policy button
        binding.btnPrivacy.setOnClickListener {
            openPrivacyPolicy()
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            binding.appVersion.text = getString(R.string.version_format, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting package info", e)
            binding.appVersion.text = getString(R.string.version_format, "Unknown")
        }
    }
    
    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val shareMessage = "Check out this amazing app: ${getString(R.string.app_name)}\n\n" +
                    "${AppConstants.PLAY_STORE_BASE_URL}${requireActivity().packageName}"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing app", e)
            Toast.makeText(requireContext(), "Unable to share app", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun rateApp() {
        try {
            val uri = Uri.parse("${AppConstants.MARKET_BASE_URL}${requireActivity().packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store app is not installed, open in browser
            val uri = Uri.parse("${AppConstants.PLAY_STORE_BASE_URL}${requireActivity().packageName}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Play Store", e)
            Toast.makeText(requireContext(), "Unable to open Play Store", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showClearHistoryConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all recent history?")
            .setPositiveButton("Yes") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun clearHistory() {
        // Implement the actual history clearing logic here
        // This might involve accessing a database or shared preferences
        
        // For example:
        // val historyManager = HistoryManager(requireContext())
        // historyManager.clearAllHistory()
        
        Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun sendFeedback() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConstants.CONTACT_EMAIL))
            intent.putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_name)} Feedback")
            intent.putExtra(Intent.EXTRA_TEXT, "App Version: ${getAppVersion()}\n\nFeedback:\n")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending feedback", e)
            Toast.makeText(requireContext(), "Unable to send feedback", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openPrivacyPolicy() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.PRIVACY_POLICY_URL))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening privacy policy", e)
            Toast.makeText(requireContext(), "Unable to open privacy policy", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
} 