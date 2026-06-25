package jiamin.chen.orangecloud.ui.zonesettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jiamin.chen.orangecloud.core.auth.AuthRepository
import jiamin.chen.orangecloud.core.auth.Scopes
import jiamin.chen.orangecloud.data.repository.ZoneSettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ZoneSettingsEvent {
    data object Purged : ZoneSettingsEvent
    data class Error(val message: String?) : ZoneSettingsEvent
}

data class ZoneSettingsUiState(
    val zoneName: String = "",
    val developmentMode: Boolean = false,
    val underAttack: Boolean = false,
    val isLoading: Boolean = false,
    val isPurging: Boolean = false,
    val missingScope: Boolean = false,
    val canWrite: Boolean = false,
    val canPurge: Boolean = false,
)

@HiltViewModel
class ZoneSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ZoneSettingsRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val hasRead = authRepository.hasScope(Scopes.ZONE_SETTINGS_READ)
    private val canWrite = authRepository.hasScope(Scopes.ZONE_SETTINGS_WRITE)
    private val canPurge = authRepository.hasScope(Scopes.CACHE_PURGE)

    private val _uiState = MutableStateFlow(
        ZoneSettingsUiState(
            zoneName = savedStateHandle.get<String>("zoneName").orEmpty(),
            isLoading = hasRead,
            missingScope = !hasRead,
            canWrite = canWrite,
            canPurge = canPurge,
        ),
    )
    val uiState: StateFlow<ZoneSettingsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ZoneSettingsEvent>(Channel.BUFFERED)
    val events: Flow<ZoneSettingsEvent> = eventChannel.receiveAsFlow()

    init {
        if (hasRead) load()
    }

    fun load() {
        if (!hasRead) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val dev = async { runCatching { repository.getSetting(zoneId, "development_mode") }.getOrNull() }
                val sec = async { runCatching { repository.getSetting(zoneId, "security_level") }.getOrNull() }
                _uiState.update {
                    it.copy(
                        developmentMode = dev.await() == "on",
                        underAttack = sec.await() == "under_attack",
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setDevelopmentMode(on: Boolean) {
        if (!canWrite) return
        _uiState.update { it.copy(developmentMode = on) }
        viewModelScope.launch {
            runCatching { repository.setSetting(zoneId, "development_mode", if (on) "on" else "off") }
                .onFailure { eventChannel.send(ZoneSettingsEvent.Error(it.message)); load() }
        }
    }

    fun setUnderAttack(on: Boolean) {
        if (!canWrite) return
        _uiState.update { it.copy(underAttack = on) }
        viewModelScope.launch {
            runCatching { repository.setSetting(zoneId, "security_level", if (on) "under_attack" else "medium") }
                .onFailure { eventChannel.send(ZoneSettingsEvent.Error(it.message)); load() }
        }
    }

    fun purgeCache() {
        if (!canPurge) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPurging = true) }
            try {
                repository.purgeAllCache(zoneId)
                eventChannel.send(ZoneSettingsEvent.Purged)
            } catch (e: Exception) {
                eventChannel.send(ZoneSettingsEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isPurging = false) }
            }
        }
    }

    /** 按 URL 清理缓存。调用方负责拆行/校验，这里只下发（单次最多 30 个）。 */
    fun purgeFiles(urls: List<String>) {
        if (!canPurge || urls.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPurging = true) }
            try {
                repository.purgeFiles(zoneId, urls.take(MAX_PURGE_URLS))
                eventChannel.send(ZoneSettingsEvent.Purged)
            } catch (e: Exception) {
                eventChannel.send(ZoneSettingsEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isPurging = false) }
            }
        }
    }

    companion object {
        const val MAX_PURGE_URLS = 30
    }
}
