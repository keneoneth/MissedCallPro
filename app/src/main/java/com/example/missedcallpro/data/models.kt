package com.example.missedcallpro.data

import android.R

data class PlanTier (
    val name: String,
    val smsLimit: Int,
    val emailLimit: Int,
    val can_edit_templates: Boolean
)

data class Quotas(
    val smsUsed: Int,
    val emailUsed: Int
) {
    fun smsLeft(limit: Int) = (limit - smsUsed).coerceAtLeast(0)
    fun emailLeft(limit: Int) = (limit - emailUsed).coerceAtLeast(0)
}

enum class TemplateType { SMS, EMAIL }

data class PlanDto(
    val id: String,
    val name: String,
    val price_cents: Int,
    val currency: String,
    val period: String,
    val sms_limit: Int,
    val email_limit: Int,
    val form_limit: Int,
    val can_edit_templates: Boolean,
    val is_active: Boolean
)