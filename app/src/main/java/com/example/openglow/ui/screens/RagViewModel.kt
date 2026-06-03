package com.example.openglow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.data.rag.RagConfig
import com.example.openglow.data.rag.RagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RagUiState(
    val isLoadingStatus: Boolean = true,
    val isSyncing: Boolean = false,
    val isAsking: Boolean = false,
    val isTestingConnection: Boolean = false,
    val serverBaseUrl: String = RagConfig.BASE_URL,
    val totalCount: Int = 0,
    val uploadedCount: Int = 0,
    val notUploadedCount: Int = 0,
    val question: String = "",
    val answer: String? = null,
    val errorMessage: String? = null,
    val syncMessage: String? = null
)

@HiltViewModel
class RagViewModel @Inject constructor(
    private val repository: RagRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RagUiState(serverBaseUrl = repository.getServerBaseUrl()))
    val uiState: StateFlow<RagUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun updateQuestion(question: String) {
        _uiState.update { it.copy(question = question, errorMessage = null) }
    }

    fun testServerConnection() {
        if (_uiState.value.isTestingConnection) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, errorMessage = null, syncMessage = null) }
            val result = repository.testServerConnection()
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    syncMessage = result.message.takeIf { result.success },
                    errorMessage = result.message.takeUnless { result.success }
                )
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStatus = true, errorMessage = null) }
            runCatching { repository.getSyncStatus() }
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            isLoadingStatus = false,
                            totalCount = status.total,
                            uploadedCount = status.uploaded,
                            notUploadedCount = status.notUploaded
                        )
                    }
                }
                .onFailure(::showError)
        }
    }

    fun syncNotifications() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, syncMessage = null) }
            runCatching { repository.syncNotifications() }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncMessage = if (result.failedCount == 0) {
                                "동기화가 완료되었습니다."
                            } else {
                                "${result.uploadedCount}개 업로드, ${result.failedCount}개 실패"
                            }
                        )
                    }
                    refreshStatus()
                }
                .onFailure(::showError)
        }
    }

    fun askQuestion() {
        if (_uiState.value.isAsking) return
        if (_uiState.value.question.isBlank()) {
            _uiState.update { it.copy(errorMessage = "질문을 입력해 주세요.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAsking = true, errorMessage = null) }
            runCatching { repository.askQuestion(_uiState.value.question) }
                .onSuccess { answer ->
                    _uiState.update { it.copy(isAsking = false, answer = answer) }
                }
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoadingStatus = false,
                isSyncing = false,
                isAsking = false,
                isTestingConnection = false,
                errorMessage = error.message
                    ?: "RAG 서버와 통신하지 못했습니다. 네트워크 상태와 서버 주소를 확인해 주세요."
            )
        }
    }
}
