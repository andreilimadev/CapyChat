package com.andreilima.capychat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andreilima.capychat.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferencesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UserPreferencesRepository(app.applicationContext)

    val darkTheme = repo.darkTheme.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    val reduceAnimations = repo.reduceAnimations.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    val vibrationEnabled = repo.vibrationEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val soundsEnabled = repo.soundsEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val experimentalEnabled = repo.experimentalEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )
    val showOnlineStatus = repo.showOnlineStatus.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val showReadReceipts = repo.showReadReceipts.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val showLastSeen = repo.showLastSeen.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    fun setDarkTheme(v: Boolean)          = viewModelScope.launch { repo.setDarkTheme(v) }
    fun setReduceAnimations(v: Boolean)   = viewModelScope.launch { repo.setReduceAnimations(v) }
    fun setVibrationEnabled(v: Boolean)   = viewModelScope.launch { repo.setVibrationEnabled(v) }
    fun setSoundsEnabled(v: Boolean)      = viewModelScope.launch { repo.setSoundsEnabled(v) }
    fun setExperimentalEnabled(v: Boolean)= viewModelScope.launch { repo.setExperimentalEnabled(v) }
    fun setShowOnlineStatus(v: Boolean)   = viewModelScope.launch { repo.setShowOnlineStatus(v) }
    fun setShowReadReceipts(v: Boolean)   = viewModelScope.launch { repo.setShowReadReceipts(v) }
    fun setShowLastSeen(v: Boolean)       = viewModelScope.launch { repo.setShowLastSeen(v) }
}