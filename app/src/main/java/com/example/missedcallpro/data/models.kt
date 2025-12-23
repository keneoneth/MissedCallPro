package com.example.missedcallpro.data

enum class PlanTier(val display: String, val smsLimit: Int, val emailLimit: Int) {
    FREE("Free", 15, 15),
    STARTER("Starter", 100, 100),
    PRO("Pro", 300, 300),
    PRO_PLUS("Pro+", 1000, 1000);

    val isPaid: Boolean get() = this != FREE
}

data class Quotas(
    val smsUsed: Int,
    val emailUsed: Int
) {
    fun smsLeft(limit: Int) = (limit - smsUsed).coerceAtLeast(0)
    fun emailLeft(limit: Int) = (limit - emailUsed).coerceAtLeast(0)
}

enum class TemplateType { SMS, EMAIL }