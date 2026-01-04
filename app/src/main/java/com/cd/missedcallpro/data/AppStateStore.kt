package com.cd.missedcallpro.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "missedcallpro_prefs")

data class AppState(
    val username: String,
    val email: String,
    val signedIn: Boolean,
    val plan: PlanState,
    val quotas: Quotas,
    val smsTemplate: String,
    val companyName: String,
    val includeFormLinkInSms: Boolean
) {
    companion object {
        fun initial(): AppState {
            return AppState(
                username = "",
                email = "",
                signedIn = false,
                plan = PlanState.initial(),
                quotas = Quotas(
                    smsUsed = 0,
                    emailUsed = 0
                ),
                smsTemplate = Defaults.SMS_TEMPLATE,
                companyName = "",
                includeFormLinkInSms = true
            )
        }
    }
}

class AppStateStore(private val context: Context) {

    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val PLAN = stringPreferencesKey("plan")
        val PLAN_STATUS = stringPreferencesKey("plan_status")
        val SMS_LIMIT = intPreferencesKey("sms_limit")
        val EMAIL_LIMIT = intPreferencesKey("email_limit")
        val EDIT_RIGHT = booleanPreferencesKey("edit_right")
        val PERIOD_START = stringPreferencesKey("period_start")
        val PERIOD_END = stringPreferencesKey("period_end")
        val SMS_USED = intPreferencesKey("sms_used")
        val EMAIL_USED = intPreferencesKey("email_used")
        val SMS_TEMPLATE = stringPreferencesKey("sms_template")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val INCLUDE_FORM_LINK_IN_SMS = booleanPreferencesKey("include_form_link_in_sms")
    }

    val state: Flow<AppState> = context.dataStore.data.map { prefs ->
        val plan = PlanState(
            name = prefs[Keys.PLAN] ?: "",
            status = prefs[Keys.PLAN_STATUS] ?: "",
            smsLimit = prefs[Keys.SMS_LIMIT] ?: 0,
            emailLimit = prefs[Keys.EMAIL_LIMIT] ?: 0,
            can_edit_templates = prefs[Keys.EDIT_RIGHT] ?: false,
            currentPeriodStart = prefs[Keys.PERIOD_START],
            currentPeriodEnd = prefs[Keys.PERIOD_END]
        )
        val username = prefs[Keys.USERNAME] ?: ""
        val email = prefs[Keys.EMAIL] ?: ""

        val signedIn = prefs[Keys.SIGNED_IN] ?: false
        val smsUsed = prefs[Keys.SMS_USED] ?: 0
        val emailUsed = prefs[Keys.EMAIL_USED] ?: 0

        val defaultSms = Defaults.SMS_TEMPLATE


        AppState(
            username = username,
            email = email,
            signedIn = signedIn,
            plan = plan,
            quotas = Quotas(smsUsed = smsUsed, emailUsed = emailUsed),
            smsTemplate = prefs[Keys.SMS_TEMPLATE] ?: defaultSms,
            companyName = prefs[Keys.COMPANY_NAME] ?: "",
            includeFormLinkInSms = prefs[Keys.INCLUDE_FORM_LINK_IN_SMS] ?: true
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
            it[Keys.PLAN_STATUS] = ""
            it[Keys.SMS_USED] = 0
            it[Keys.EMAIL_USED] = 0
            it.remove(Keys.PERIOD_START)
            it.remove(Keys.PERIOD_END)
            it.remove(Keys.SMS_TEMPLATE)

        }
    }

    suspend fun setPlan(
        plan: PlanState
    ) {
        context.dataStore.edit {
            it[Keys.PLAN] = plan.name

            it[Keys.SMS_LIMIT] = plan.smsLimit
            it[Keys.EMAIL_LIMIT] = plan.emailLimit
            it[Keys.EDIT_RIGHT] = plan.can_edit_templates

            if (plan.status != null)
                it[Keys.PLAN_STATUS] = plan.status
            else
                it.remove(Keys.PLAN_STATUS)

            if (plan.currentPeriodStart != null)
                it[Keys.PERIOD_START] = plan.currentPeriodStart
            else
                it.remove(Keys.PERIOD_START)

            if (plan.currentPeriodEnd != null)
                it[Keys.PERIOD_END] = plan.currentPeriodEnd
            else
                it.remove(Keys.PERIOD_END)
        }
    }

    suspend fun setQuotas(smsUsed: Int, emailUsed: Int) {
        context.dataStore.edit {
            it[Keys.SMS_USED] = smsUsed
            it[Keys.EMAIL_USED] = emailUsed
        }
    }
    suspend fun getCompanyName(): String {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.COMPANY_NAME] ?: ""
    }
    suspend fun saveSmsSettings(
        companyName: String,
        template: String,
        includeFormLinkInSms: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.COMPANY_NAME] = companyName
            it[Keys.SMS_TEMPLATE] = template
            it[Keys.INCLUDE_FORM_LINK_IN_SMS] = includeFormLinkInSms
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