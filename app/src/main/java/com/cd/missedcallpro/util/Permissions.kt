package com.cd.missedcallpro.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class PhonePermState(
    val hasPhoneState: Boolean,
    val hasCallLog: Boolean
) {
    val allGranted: Boolean get() = hasPhoneState && hasCallLog
}

fun getPhonePermState(ctx: Context): PhonePermState {
    val phone = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val log = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    return PhonePermState(phone, log)
}