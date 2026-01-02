// telephony/PhoneStateReceiver.kt
package com.example.missedcallpro.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"

        // Simple state machine
        private var lastState: String? = null
        private var sawRinging = false
        private var sawOffhook = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PhoneStateReceiver", "onReceive action=${intent.action} extras=${intent.extras}")

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        if (state == lastState) return
        lastState = state

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                sawRinging = true
                sawOffhook = false
                Log.d(TAG, "RINGING")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered or outgoing connected
                if (sawRinging) {
                    sawOffhook = true
                }
                Log.d(TAG, "OFFHOOK")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d(TAG, "IDLE")
                // Missed call: rang but never went offhook
                if (sawRinging && !sawOffhook) {
                    Log.d(TAG, "Detected MISSED call pattern -> enqueue worker")
                    val req = OneTimeWorkRequestBuilder<MissedCallWorker>()
                        .setInputData(workDataOf("event_ts_ms" to System.currentTimeMillis()))
                        .build()
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "missed_call_report",
                        ExistingWorkPolicy.REPLACE,
                        req
                    )
                }
                sawRinging = false
                sawOffhook = false
            }
        }
    }
}
