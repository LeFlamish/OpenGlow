package com.example.openglow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openglow.BuildConfig
import com.example.openglow.data.localai.LocalLlmClient
import com.example.openglow.data.model.AppModelInfo
import com.example.openglow.data.model.ModelDownloadManager
import com.example.openglow.data.model.ModelDownloadState
import com.example.openglow.data.model.ModelRegistry
import com.example.openglow.data.remote.GeminiLlmClient
import com.example.openglow.domain.classifier.ClassifierRouter
import com.example.openglow.domain.llm.NoteUpdateInput
import com.example.openglow.domain.llm.PersonalizationRules
import com.example.openglow.domain.llm.SenderScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelSetupUiState(
    val geminiApiKeyConfigured: Boolean = false,
    val localLlmState: ModelDownloadState = ModelDownloadState.NotDownloaded,
    val kcElectraState: ModelDownloadState = ModelDownloadState.NotDownloaded,
    val localLlmPath: String? = null,
    val kcElectraPath: String? = null,
    val currentClassifier: String = "RuleBased",
    val currentLlmFallback: String = "RuleBased",
    val testMessage: String? = null,
)

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val modelDownloadManager: ModelDownloadManager,
    private val geminiLlmClient: GeminiLlmClient,
    private val localLlmClient: LocalLlmClient,
    private val classifierRouter: ClassifierRouter,
) : ViewModel() {
    private val testMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModelSetupUiState> = combine(
        modelDownloadManager.getModelStateFlow(ModelRegistry.recommendedLocalLlm.id),
        modelDownloadManager.getModelStateFlow(ModelRegistry.recommendedKcElectra.id),
        testMessage,
    ) { localState, kcState, message ->
        val localReady = modelDownloadManager.isModelReady(ModelRegistry.recommendedLocalLlm)
        val kcReady = modelDownloadManager.isModelReady(ModelRegistry.recommendedKcElectra)
        ModelSetupUiState(
            geminiApiKeyConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            localLlmState = localState,
            kcElectraState = kcState,
            localLlmPath = modelDownloadManager.getModelDirectory(ModelRegistry.recommendedLocalLlm).absolutePath,
            kcElectraPath = modelDownloadManager.getModelDirectory(ModelRegistry.recommendedKcElectra).absolutePath,
            currentClassifier = if (kcReady) "KcELECTRA" else "RuleBased",
            currentLlmFallback = if (localReady && BuildConfig.LOCAL_LLM_ENABLED) "Local LLM" else "RuleBased",
            testMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ModelSetupUiState(
            geminiApiKeyConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            localLlmPath = modelDownloadManager.getModelDirectory(ModelRegistry.recommendedLocalLlm).absolutePath,
            kcElectraPath = modelDownloadManager.getModelDirectory(ModelRegistry.recommendedKcElectra).absolutePath,
        ),
    )

    fun downloadLocalLlm() = download(ModelRegistry.recommendedLocalLlm)
    fun downloadKcElectra() = download(ModelRegistry.recommendedKcElectra)
    fun deleteLocalLlm() = delete(ModelRegistry.recommendedLocalLlm)
    fun deleteKcElectra() = delete(ModelRegistry.recommendedKcElectra)
    fun cancelLocalLlm() = modelDownloadManager.cancelDownload(ModelRegistry.recommendedLocalLlm.id)
    fun cancelKcElectra() = modelDownloadManager.cancelDownload(ModelRegistry.recommendedKcElectra.id)
    fun clearTestMessage() {
        testMessage.value = null
    }

    fun testGemini() {
        viewModelScope.launch {
            val input = testInput(classifierRouter.classify(TEST_TEXT))
            val result = geminiLlmClient.analyze(input)
            testMessage.value = result.fold(
                onSuccess = { "Gemini 테스트 성공: ${it.oneLineSummary.take(60)}" },
                onFailure = { "Gemini 테스트 실패: ${it.message}" },
            )
        }
    }

    fun testLocalLlm() {
        viewModelScope.launch {
            val input = testInput(classifierRouter.classify(TEST_TEXT))
            val result = if (localLlmClient.isAvailable()) {
                localLlmClient.analyze(input)
            } else {
                Result.failure(IllegalStateException("Local LLM unavailable"))
            }
            testMessage.value = result.fold(
                onSuccess = { "Local LLM 테스트 성공" },
                onFailure = { "Local LLM 테스트 실패: ${it.message}" },
            )
        }
    }

    fun testClassifier() {
        viewModelScope.launch {
            val hint = classifierRouter.classify(TEST_TEXT)
            testMessage.value = "분류 테스트: ${hint.modelName}, importance=${hint.importanceHint}, work=${hint.workRelatedScore}"
        }
    }

    private fun download(model: AppModelInfo) {
        viewModelScope.launch {
            modelDownloadManager.downloadModel(model)
        }
    }

    private fun delete(model: AppModelInfo) {
        viewModelScope.launch {
            modelDownloadManager.deleteModel(model)
        }
    }

    private fun testInput(hint: com.example.openglow.domain.classifier.ClassificationHint): NoteUpdateInput {
        return NoteUpdateInput(
            platform = "TEST",
            packageName = "com.example.openglow.test",
            senderName = "테스트",
            senderDisplayName = "모델 테스트",
            senderScope = SenderScope.INDIVIDUAL,
            previousFinalSummary = "내일 오전 프로젝트 회의 준비가 필요함.",
            previousRetainedFacts = listOf("내일 오전 프로젝트 회의"),
            newNotificationText = TEST_TEXT,
            textFragments = listOf(TEST_TEXT),
            completenessConfidence = 0.9f,
            classificationHint = hint,
            timestamp = System.currentTimeMillis(),
            userPersonalizationRules = PersonalizationRules(),
        )
    }

    private companion object {
        private const val TEST_TEXT = "내일 오전 10시 프로젝트 회의 자료를 확인해 주세요."
    }
}
