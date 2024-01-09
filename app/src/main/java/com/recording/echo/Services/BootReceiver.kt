package com.recording.echo.Services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.recording.echo.Activities.SplashScreen

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val startAppIntent = Intent(context, SplashScreen::class.java)
            startAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(startAppIntent)
        }
    }
}
