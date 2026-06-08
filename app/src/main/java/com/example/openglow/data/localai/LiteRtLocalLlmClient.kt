package com.example.openglow.data.localai

import android.content.Context
import android.util.Log
import com.example.openglow.BuildConfig
import com.example.openglow.data.model.ModelDownloadManager
import com.example.openglow.domain.llm.AnalysisPromptBuilder
import com.example.openglow.domain.llm.NoteUpdateInput
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.lang.reflect.InvocationTargetException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LiteRtLocalLlmClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager,
) : LocalLlmClient {
    private val engineMutex = Mutex()

    @Volatile private var cachedModelFile: File? = null
    @Volatile private var cachedEngine: Any? = null
    @Volatile private var cachedEngineModelPath: String? = null

    override suspend fun isAvailable(): Boolean {
        val modelFile = resolveModelFile()
        val available = BuildConfig.LOCAL_LLM_ENABLED && modelFile != null
        Log.i(
            TAG,
            "Local LLM availability: enabled=${BuildConfig.LOCAL_LLM_ENABLED}, " +
                "backend=${BuildConfig.LOCAL_LLM_BACKEND}, available=$available",
        )
        return available
    }

    override suspend fun analyze(input: NoteUpdateInput): Result<String> {
        if (!BuildConfig.LOCAL_LLM_ENABLED) {
            return Result.failure(IllegalStateException("Local LLM is disabled"))
        }
        val modelFile = resolveModelFile()
            ?: return Result.failure(IllegalStateException("Local LLM model is not available"))

        return withContext(Dispatchers.IO) {
            runCatching {
                val prompt = AnalysisPromptBuilder.buildLocalPrompt(input)
                Log.i(
                    TAG,
                    "Local LLM request: backend=${BuildConfig.LOCAL_LLM_BACKEND}, modelBytes=${modelFile.length()}",
                )
                engineMutex.withLock {
                    val engine = getOrCreateEngine(modelFile)
                    runConversation(engine, prompt).ifBlank {
                        error("Local LLM returned an empty response")
                    }
                }
            }
        }
    }

    fun clearEngineCache() {
        cachedModelFile = null
        cachedEngineModelPath = null
        closeQuietly(cachedEngine)
        cachedEngine = null
    }

    private fun getOrCreateEngine(modelFile: File): Any {
        val modelPath = modelFile.absolutePath
        cachedEngine
            ?.takeIf { cachedEngineModelPath == modelPath }
            ?.let { return it }

        closeQuietly(cachedEngine)
        cachedEngine = null
        cachedEngineModelPath = null

        var lastError: Throwable? = null
        preferredBackendNames().forEach { backendName ->
            val engine = runCatching {
                createEngine(modelPath = modelPath, backendName = backendName)
            }.onFailure {
                lastError = unwrapReflectionError(it)
                Log.w(TAG, "LiteRT-LM backend failed: $backendName, reason=${lastError?.message}")
            }.getOrNull()

            if (engine != null) {
                cachedEngine = engine
                cachedEngineModelPath = modelPath
                Log.i(TAG, "LiteRT-LM engine initialized: backend=$backendName")
                return engine
            }
        }

        throw IllegalStateException("LiteRT-LM engine initialization failed", lastError)
    }

    private fun createEngine(modelPath: String, backendName: String): Any {
        val backendClass = Class.forName("$LITERT_PACKAGE.Backend")
        val backend = Class.forName("$LITERT_PACKAGE.Backend\$$backendName")
            .getConstructor()
            .newInstance()
        val engineConfigClass = Class.forName("$LITERT_PACKAGE.EngineConfig")
        val engineConfig = engineConfigClass
            .getConstructor(
                String::class.java,
                backendClass,
                backendClass,
                backendClass,
                Integer::class.java,
                Integer::class.java,
                String::class.java,
            )
            .newInstance(
                modelPath,
                backend,
                null,
                null,
                null,
                null,
                context.cacheDir.absolutePath,
            )
        val engine = Class.forName("$LITERT_PACKAGE.Engine")
            .getConstructor(engineConfigClass)
            .newInstance(engineConfig)
        engine.javaClass.getMethod("initialize").invoke(engine)
        return engine
    }

    private suspend fun runConversation(engine: Any, prompt: String): String {
        val conversationConfigClass = Class.forName("$LITERT_PACKAGE.ConversationConfig")
        val conversationConfig = conversationConfigClass.getConstructor().newInstance()
        val conversation = engine.javaClass
            .getMethod("createConversation", conversationConfigClass)
            .invoke(engine, conversationConfig)
            ?: error("LiteRT-LM failed to create a conversation")
        return try {
            val flow = conversation.javaClass
                .getMethod("sendMessageAsync", String::class.java, Map::class.java)
                .invoke(conversation, prompt, emptyMap<String, Any>()) as Flow<*>
            val response = StringBuilder()
            flow.collect { message ->
                response.append(renderMessage(conversation, message))
            }
            response.toString().trim()
        } finally {
            closeQuietly(conversation)
        }
    }

    private fun renderMessage(conversation: Any, message: Any?): String {
        if (message == null) return ""
        val messageClass = Class.forName("$LITERT_PACKAGE.Message")
        return runCatching {
            conversation.javaClass
                .getMethod("renderMessageIntoString", messageClass, Map::class.java)
                .invoke(conversation, message, emptyMap<String, Any>())
                ?.toString()
                .orEmpty()
        }.getOrElse {
            message.toString()
        }
    }

    private fun preferredBackendNames(): List<String> {
        return when (BuildConfig.LOCAL_LLM_BACKEND.trim().uppercase()) {
            "CPU" -> listOf("CPU")
            else -> listOf("GPU", "CPU")
        }
    }

    private fun resolveModelFile(): File? {
        val debugFile = BuildConfig.DEBUG_LOCAL_LLM_MODEL_PATH
            .takeIf { BuildConfig.DEBUG && it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() && it.isFile && it.length() > 0L }
        if (debugFile != null) {
            cachedModelFile = debugFile
            return debugFile
        }

        val downloaded = modelDownloadManager.getLocalLlmModelFile()
        cachedModelFile = downloaded
        return downloaded
    }

    private fun closeQuietly(instance: Any?) {
        runCatching {
            (instance as? AutoCloseable)?.close()
        }
    }

    private fun unwrapReflectionError(error: Throwable): Throwable {
        return if (error is InvocationTargetException) {
            error.targetException ?: error
        } else {
            error
        }
    }

    private companion object {
        private const val TAG = "LocalLlmClient"
        private const val LITERT_PACKAGE = "com.google.ai.edge.litertlm"
    }
}
