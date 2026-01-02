package com.example.missedcallpro.data

import com.squareup.moshi.JsonClass

data class PlanState(
    val name: String,
    val smsLimit: Int,
    val emailLimit: Int,
    val can_edit_templates: Boolean,
    val status: String? = null,
    val currentPeriodStart: String? = null,
    val currentPeriodEnd: String? = null
) {
    companion object {
        fun initial(): PlanState {
            return PlanState(
                name = "Free",
                smsLimit = 0,
                emailLimit = 0,
                can_edit_templates = false,
                currentPeriodStart = null,
                currentPeriodEnd = null,
                status = "free"
            )
        }
    }
}

data class Quotas(
    val smsUsed: Int,
    val emailUsed: Int
) {
    fun smsLeft(limit: Int) = (limit - smsUsed).coerceAtLeast(0)
    fun emailLeft(limit: Int) = (limit - emailUsed).coerceAtLeast(0)
}

data class PlanDto(
    val id: String,
    val name: String,
    val price_cents: Int,
    val currency: String,
    val period: String,
    val sms_limit: Int,
    val email_limit: Int,
    val can_edit_templates: Boolean,
    val is_active: Boolean,
    val play_product_id: String?
)

data class QuotaDto(
    val limit: Int,
    val used: Int,
    val remaining: Int
)

data class SubscriptionDto(
    val plan_id: String,
    val status: String,
    val current_period_start: String?, // ISO string, nullable
    val current_period_end: String?,
    val sms: QuotaDto,
    val email: QuotaDto
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val PLAN_FREE = "free"
    }
}

data class GoogleConfirmReq(
    val product_id: String,
    val purchase_token: String,
    val package_name: String? = null
)

data class GoogleConfirmResp(
    val ok: Boolean,
    val plan_id: String,
    val status: String,
    val cancel_at_period_end: Boolean,
    val current_period_end: String?
)

data class GoogleRestoreItemReq(
    val product_id: String,
    val purchase_token: String,
    val package_name: String? = null
)

data class GoogleRestoreReq(
    val purchases: List<GoogleRestoreItemReq>,
    val package_name: String? = null
)

data class GoogleRestoreResp(
    val ok: Boolean,
    val restored: Boolean,
    val plan_id: String,
    val status: String,
    val current_period_end: String? = null
)

data class FilterUiState(
    val mode: String = "none",            // "none" | "allow" | "block"
    val callingCodes: List<String> = emptyList(),
    val blockedNumbers: List<FilterNumberOut> = emptyList()
)

data class FilterRuleUpdate(
    val mode: String,
    val calling_codes: List<String>
)

data class FilterNumberOut(
    val id: Long,
    val e164_number: String,
    val label: String?
)
data class FilterNumberCreate(
    val e164_number: String,
    val label: String? = null
)

data class FeedbackCreate(
    val message: String,
    val app_version: String? = null,
    val platform: String? = "android",
    val device_id: String? = null,
    val locale: String? = null,
)

data class FeedbackResponse(
    val ok: Boolean,
    val remaining_today: Int,
)

data class DeviceMissedCallRequest(
    val from_number: String,
    val occurred_at_ms: Long
)

data class DeviceMissedCallResponse(
    val ok: Boolean = true,
    val sms_used: Int,
    val email_used: Int
)

data class SmsSettingsDto(
    val company_name: String,
    val sms_template: String,
    val include_form_link: Boolean
)

@JsonClass(generateAdapter = true)
data class FormResponseDto(
    val form: FormDefinitionDto,
    val is_locked: Boolean
)

@JsonClass(generateAdapter = true)
data class FormUpdateRequestDto(
    val form: FormDefinitionDto
)

@JsonClass(generateAdapter = true)
data class FormDefinitionDto(
    val version: Int = 1,
    val title: String = "Client Info",
    val fields: List<FormFieldDto>,
    val meta: FormMetaDto? = null
)

@JsonClass(generateAdapter = true)
data class FormMetaDto(
    val created_at_ms: Long = 0,
    val updated_at_ms: Long = 0
)

@JsonClass(generateAdapter = true)
data class FormFieldDto(
    val id: String,
    val label: String,
    val type: String, // "text" | "number" | "integer" | "mc" | "phone"
    val required: Boolean = false,
    val placeholder: String? = null,
    val options: List<McOptionDto>? = null,
    val max_choices: Int? = null
)

@JsonClass(generateAdapter = true)
data class McOptionDto(
    val id: String,
    val label: String
)