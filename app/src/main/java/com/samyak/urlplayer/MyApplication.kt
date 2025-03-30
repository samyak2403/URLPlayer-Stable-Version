package com.samyak.urlplayer

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.FirebaseDatabase
import com.samyak.urlplayer.AdManage.loadAdUnits
import com.samyak.urlplayer.AdManage.loadInterstitialAdIfNull

class MyApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        MobileAds.initialize(this) {
            loadAdUnits {
                loadInterstitialAdIfNull(this)

            }
        }


    }
}