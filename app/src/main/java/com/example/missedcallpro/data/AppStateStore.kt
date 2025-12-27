package com.example.missedcallpro.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "missedcallpro_prefs")

data class AppState(
    val username: String,
    val email: String,
    val signedIn: Boolean,
    val plan: PlanTier,
    val quotas: Quotas,
    val smsTemplate: String,
    val emailTemplate: String
)

class AppStateStore(private val context: Context) {

    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val PLAN = stringPreferencesKey("plan")
        val SMS_USED = intPreferencesKey("sms_used")
        val EMAIL_USED = intPreferencesKey("email_used")
        val SMS_TEMPLATE = stringPreferencesKey("sms_template")
        val EMAIL_TEMPLATE = stringPreferencesKey("email_template")
    }

    val state: Flow<AppState> = context.dataStore.data.map { prefs ->
        val plan = PlanTier(prefs[Keys.PLAN] ?: "",0,0, false)
        val username = prefs[Keys.USERNAME] ?: ""
        val email = prefs[Keys.EMAIL] ?: ""

        val signedIn = prefs[Keys.SIGNED_IN] ?: false
        val smsUsed = prefs[Keys.SMS_USED] ?: 0
        val emailUsed = prefs[Keys.EMAIL_USED] ?: 0

        val defaultSms = Defaults.SMS_TEMPLATE
        val defaultEmail = Defaults.EMAIL_TEMPLATE

        AppState(
            username = username,
            email = email,
            signedIn = signedIn,
            plan = plan,
            quotas = Quotas(smsUsed = smsUsed, emailUsed = emailUsed),
            smsTemplate = prefs[Keys.SMS_TEMPLATE] ?: defaultSms,
            emailTemplate = prefs[Keys.EMAIL_TEMPLATE] ?: defaultEmail
        )
    }

    suspend fun setUsername(value: String) {
        context.dataStore.edit {
            it[Keys.USERNAME] = value;
        }
    }
    suspend fun setEmail(value: String) {
        context.dataStore.edit {
            it[Keys.EMAIL] = value;
        }
    }

    suspend fun signIn() {
        context.dataStore.edit { it[Keys.SIGNED_IN] = true }
    }

    suspend fun signOut() {
        context.dataStore.edit {
            it[Keys.SIGNED_IN] = false
            it[Keys.PLAN] = ""
            it[Keys.SMS_USED] = 0
            it[Keys.EMAIL_USED] = 0
            it.remove(Keys.SMS_TEMPLATE)
            it.remove(Keys.EMAIL_TEMPLATE)
        }
    }

    suspend fun setPlan(plan: PlanTier) {
        context.dataStore.edit {
            Log.d("Update plan name", plan.name)
            it[Keys.PLAN] = plan.name
        }
    }

    suspend fun setTemplate(type: TemplateType, value: String) {
        context.dataStore.edit {
            when (type) {
                TemplateType.SMS -> it[Keys.SMS_TEMPLATE] = value
                TemplateType.EMAIL -> it[Keys.EMAIL_TEMPLATE] = value
            }
        }
    }

    suspend fun consumeSms(count: Int = 1) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SMS_USED] ?: 0
            prefs[Keys.SMS_USED] = current + count
        }
    }

    suspend fun consumeEmail(count: Int = 1) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EMAIL_USED] ?: 0
            prefs[Keys.EMAIL_USED] = current + count
        }
    }

}