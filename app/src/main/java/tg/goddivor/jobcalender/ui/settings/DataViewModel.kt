package tg.goddivor.jobcalender.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.remote.SyncOutbox
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import javax.inject.Inject

data class DataUiState(
    val applicationCount: Int = 0,
    val eventCount: Int = 0,
    val pendingWrites: Int = 0,
)

/**
 * What this device holds, and what it still owes the server. There is nothing to import or export:
 * the base lives on the server, fed by the jobing MCP, and this copy is what was last pulled.
 */
@HiltViewModel
class DataViewModel @Inject constructor(
    private val applications: ApplicationRepository,
    private val events: EventRepository,
    private val outbox: SyncOutbox,
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    applicationCount = applications.count(),
                    eventCount = events.count(),
                    pendingWrites = outbox.pending(),
                )
            }
        }
    }
}
