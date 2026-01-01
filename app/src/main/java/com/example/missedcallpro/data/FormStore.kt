package com.example.missedcallpro.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FormState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val form: FormDefinitionDto? = null,
    val isLocked: Boolean = true
)

class FormStore {
    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    fun setLoading() { _state.value = _state.value.copy(isLoading = true, error = null) }
    fun setError(msg: String) { _state.value = _state.value.copy(isLoading = false, error = msg) }
    fun setForm(form: FormDefinitionDto, locked: Boolean) {
        _state.value = _state.value.copy(isLoading = false, error = null, form = form, isLocked = locked)
    }

}