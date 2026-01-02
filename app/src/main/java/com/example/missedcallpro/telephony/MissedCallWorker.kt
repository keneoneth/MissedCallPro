// telephony/MissedCallWorker.kt
package com.example.missedcallpro.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.missedcallpro.App
import com.example.missedcallpro.data.DeviceMissedCallRequest

class MissedCallWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    val app = appContext.applicationContext as App
    val api = app.container.api
    companion object {
        private const val TAG = "MissedCallWorker"
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext

        // Permissions check
        val hasCallLog = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        if (!hasCallLog) {
            Log.w(TAG, "No READ_CALL_LOG permission; cannot extract missed number.")
            return Result.success() // don’t retry forever
        }

        val missedNumber = queryLatestMissedNumber(ctx) ?: run {
            Log.w(TAG, "No missed call found in call log.")
            return Result.success()
        }

        try {
            val timestamp = inputData.getLong("event_ts_ms", 0L)
            val resp = api.reportDeviceMissedCall(
                body = DeviceMissedCallRequest(
                    from_number = missedNumber,
                    occurred_at_ms = timestamp,
                )
            )
            app.store.setQuotas(
                smsUsed = resp.sms_used,
                emailUsed = resp.email_used
            )
            Log.d(TAG, "Reported missed call: $missedNumber")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report missed call", e)
            // Network/server issue: retry
            return Result.retry()
        }
    }

    private fun queryLatestMissedNumber(ctx: Context): String? {
        val TAG = "MissedCallWorker"

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )

        val selection = "${CallLog.Calls.TYPE}=?"
        val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString())

        val sortOrder = "${CallLog.Calls.DATE} DESC"

        return try {
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    Log.d(TAG, "CallLog query returned empty cursor")
                    return null
                }

                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                if (numberIdx < 0) {
                    Log.d(TAG, "CallLog NUMBER column not found")
                    return null
                }

                val number = cursor.getString(numberIdx)
                Log.d(TAG, "Latest missed call number = $number")
                number
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException querying CallLog (permission revoked?)", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException querying CallLog (bad query)", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception querying CallLog", e)
            null
        }
    }


}
