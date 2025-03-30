package com.samyak.urlplayer.utils

import android.app.Activity
import android.content.Context
import java.util.Locale

object LanguageManager {
    
    // Get current language code
    fun getCurrentLanguage(context: Context): String {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
    }
    
    // Set language and restart activity
    fun setLanguage(activity: Activity, languageCode: String) {
        // Save language preference
        activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("language", languageCode)
            .apply()
        
        // Update locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = activity.resources.configuration
        config.setLocale(locale)
        
        // Update configuration
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        
        // Restart activity
        activity.recreate()
    }
    
    // Get language name from code
    fun getLanguageName(languageCode: String): String {
        val locale = Locale(languageCode)
        return locale.displayLanguage.capitalize(Locale.getDefault())
    }
    
    // Get all supported languages
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            "English" to "en",
            "हिंदी (Hindi)" to "hi",
            "मराठी (Marathi)" to "mr",
            "español (Spanish)" to "es",
            "français (French)" to "fr",
            "Deutsche (German)" to "de",
            "italiano (Italian)" to "it",
            "português (Portuguese)" to "pt",
            "русский (Russian)" to "ru",
            "日本語 (Japanese)" to "ja",
            "한국어 (Korean)" to "ko",
            "中文 (Chinese)" to "zh",
            "العربية (Arabic)" to "ar",
            "Türkçe (Turkish)" to "tr",
            "Nederlands (Dutch)" to "nl",
            "Polski (Polish)" to "pl",
            "Bahasa Indonesia" to "in",
            "ไทย (Thai)" to "th",
            "Tiếng Việt (Vietnamese)" to "vi"
        )
    }
    
    // Check if language is RTL
    fun isRtlLanguage(languageCode: String): Boolean {
        return languageCode == "ar"
    }
} 