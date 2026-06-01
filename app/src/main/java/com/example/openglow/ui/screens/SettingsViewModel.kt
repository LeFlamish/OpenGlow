package com.example.openglow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.domain.training.TrainingDataExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val trainingDataExporter: TrainingDataExporter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun exportTrainingData(includeTextPreview: Boolean) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isExporting = true)
            val result = trainingDataExporter.exportJsonl(includeTextPreview = includeTextPreview)
            _uiState.value = SettingsUiState(
                isExporting = false,
                exportMessage = result.fold(
                    onSuccess = { "내보내기 완료: ${it.absolutePath}" },
                    onFailure = { "내보내기 실패: ${it.message}" },
                ),
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }
}
