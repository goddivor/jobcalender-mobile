package tg.goddivor.jobcalender.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.backup.BackupManager
import tg.goddivor.jobcalender.data.backup.BackupResult
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import javax.inject.Inject

data class DataUiState(
    val applicationCount: Int = 0,
    val eventCount: Int = 0,
    val busy: Boolean = false,
    val lastResult: BackupResult? = null,
)

@HiltViewModel
class DataViewModel @Inject constructor(
    private val backups: BackupManager,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state = _state.asStateFlow()

    init {
        refreshCounts()
    }

    fun export(target: Uri) = perform { backups.export(target) }

    fun import(source: Uri) = perform { backups.import(source) }

    private fun perform(work: suspend () -> BackupResult) = viewModelScope.launch {
        _state.update { it.copy(busy = true, lastResult = null) }
        val result = work()
        _state.update { it.copy(busy = false, lastResult = result) }
        refreshCounts()
    }

    private fun refreshCounts() = viewModelScope.launch {
        _state.update { it.copy(applicationCount = applications.count(), eventCount = events.count()) }
    }
}
