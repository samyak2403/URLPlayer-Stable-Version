package com.samyak.urlplayer

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.samyak.urlplayer.screen.PlayerActivity

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        // Define notification actions
        val buttonActions = listOf(
            MediaIntentReceiver.ACTION_SKIP_NEXT,
            MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
            MediaIntentReceiver.ACTION_STOP_CASTING
        )
        
        // Create notification options with actions - convert List<Int> to IntArray
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(context.packageName + ".screen.PlayerActivity")
            .setActions(buttonActions, intArrayOf(0, 1, 2))
            .build()
        
        // Create media options with expanded controller
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(context.packageName + ".screen.PlayerActivity")
            .build()

        // Build and return cast options
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(mediaOptions)
            .setResumeSavedSession(true)  // Resume session if available
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
} 