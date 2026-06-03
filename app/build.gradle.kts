import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun String.asBuildConfigString(): String = replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.openglow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.openglow"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
            ?: project.findProperty("GEMINI_API_KEY") as? String
            ?: ""
        val geminiModel = localProperties.getProperty("GEMINI_MODEL")
            ?: project.findProperty("GEMINI_MODEL") as? String
            ?: "gemini-3.5-flash"
        val localLlmModelPath = localProperties.getProperty("LOCAL_LLM_MODEL_PATH")
            ?: project.findProperty("LOCAL_LLM_MODEL_PATH") as? String
            ?: ""
        val enableLocalLlm = localProperties.getProperty("ENABLE_LOCAL_LLM")
            ?: project.findProperty("ENABLE_LOCAL_LLM") as? String
            ?: "false"
        val localLlmBackend = localProperties.getProperty("LOCAL_LLM_BACKEND")
            ?: project.findProperty("LOCAL_LLM_BACKEND") as? String
            ?: "litertlm"
        val modelRegistryBaseUrl = localProperties.getProperty("MODEL_REGISTRY_BASE_URL")
            ?: project.findProperty("MODEL_REGISTRY_BASE_URL") as? String
            ?: ""
        val ragApiKey = localProperties.getProperty("RAG_API_KEY")
            ?: project.findProperty("RAG_API_KEY") as? String
            ?: ""

        // Prototype only: route Gemini calls through a backend before release.
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey.asBuildConfigString()}\"")
        buildConfigField("String", "GEMINI_MODEL", "\"${geminiModel.asBuildConfigString()}\"")
        buildConfigField("String", "LOCAL_LLM_MODEL_PATH", "\"${localLlmModelPath.asBuildConfigString()}\"")
        buildConfigField("String", "DEBUG_LOCAL_LLM_MODEL_PATH", "\"${localLlmModelPath.asBuildConfigString()}\"")
        buildConfigField("Boolean", "ENABLE_LOCAL_LLM", enableLocalLlm.toBooleanStrictOrNull()?.toString() ?: "false")
        buildConfigField("Boolean", "LOCAL_LLM_ENABLED", enableLocalLlm.toBooleanStrictOrNull()?.toString() ?: "false")
        buildConfigField("String", "LOCAL_LLM_BACKEND", "\"${localLlmBackend.asBuildConfigString()}\"")
        buildConfigField("String", "MODEL_REGISTRY_BASE_URL", "\"${modelRegistryBaseUrl.asBuildConfigString()}\"")
        buildConfigField("String", "RAG_API_KEY", "\"${ragApiKey.asBuildConfigString()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.onnxruntime.android)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
