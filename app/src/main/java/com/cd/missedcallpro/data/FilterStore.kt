package com.cd.missedcallpro.data

import com.cd.missedcallpro.data.remote.*

class FilterStore(private val api: BackendApi) {

    suspend fun load(): FilterUiState {
        val rule = api.getFilterRule()
        val blocked = api.listFilterNumbers()

        return FilterUiState(
            mode = rule.mode,
            callingCodes  = rule.calling_codes.sorted(),
            blockedNumbers = blocked.sortedBy { it.e164_number },
        )
    }

    suspend fun saveModeAndCodes(mode: String, callingCodes: List<String>) {
        api.putFilterRule(
            FilterRuleUpdate(
                mode = mode,
                calling_codes = callingCodes
            )
        )
    }

    suspend fun addBlockedNumber(e164: String) {
        api.addFilterNumber(FilterNumberCreate(e164))
    }

    suspend fun deleteBlockedNumber(id: Long) {
        api.deleteFilterNumber(id)
    }
}

