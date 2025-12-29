package com.example.missedcallpro.data

// ui/feedback/FeedbackViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.missedcallpro.data.remote.BackendApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class FeedbackUiState(
    val text: String = "",
    val isSending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
    val remainingToday: Int? = null,
)

class FeedbackViewModel(
    private val appVersion: String?,
    private val deviceId: String?,
    private val locale: String?,
    private val api: BackendApi,
) : ViewModel() {

    val maxChars = 2000
    private val _state = MutableStateFlow(FeedbackUiState())
    val state = _state.asStateFlow()


    fun onTextChange(newText: String) {
        _state.value = _state.value.copy(
            text = newText.take(maxChars),
            error = null,
            sent = false
        )
    }

    fun send() {
        val msg = _state.value.text.trim()
        if (msg.isEmpty()) {
            _state.value = _state.value.copy(error = "Please write something first.")
            return
        }
        _state.value = _state.value.copy(isSending = true, error = null)

        viewModelScope.launch {
            try {
                val resp = api.submitFeedback(
                    FeedbackCreate(
                        message = msg,
                        app_version = appVersion,
                        device_id = deviceId,
                        locale = locale
                    )
                )
                _state.value = _state.value.copy(
                    isSending = false,
                    sent = true,
                    remainingToday = resp.remaining_today,
                    text = "" // clear
                )
            } catch (e: HttpException) {
                val msgErr = if (e.code() == 429) {
                    "You hit the daily limit (3/day). Try again tomorrow."
                } else {
                    "Failed to send (HTTP ${e.code()})"
                }
                _state.value = _state.value.copy(isSending = false, error = msgErr)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSending = false, error = e.message ?: "Failed to send")
            }
        }
    }
}

class FeedbackViewModelFactory(
    private val appVersion: String?,
    private val deviceId: String?,
    private val locale: String?,
    private val api: BackendApi,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
            return FeedbackViewModel(appVersion, deviceId, locale, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
