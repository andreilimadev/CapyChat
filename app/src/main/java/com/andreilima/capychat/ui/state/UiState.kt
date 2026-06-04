package com.andreilima.capychat.ui.state

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

fun <T> UiState<T>.isLoading() = this is UiState.Loading
fun <T> UiState<T>.isEmpty() = this is UiState.Empty
fun <T> UiState<T>.getDataOrNull() = (this as? UiState.Success)?.data
fun <T> UiState<T>.getErrorOrNull() = (this as? UiState.Error)?.message