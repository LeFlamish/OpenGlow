# OpenGlow

## Local Setup

Keep local-only keys and model settings in `local.properties`. This file is ignored by Git.

```properties
sdk.dir=C\:\\Users\\jiuk0\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-3.5-flash
ENABLE_LOCAL_LLM=true
LOCAL_LLM_BACKEND=GPU
MODEL_MANIFEST_URL=https://github.com/LeFlamish/OpenGlow/releases/download/models-v1/model_manifest.json
```

`MODEL_MANIFEST_URL` is preferred over the built-in placeholder registry. If it is
missing, or if a model artifact has no URL or SHA-256, the setup screen shows
`NotConfigured` instead of trying a broken download.

## Model Manifest

OpenGlow downloads large model files into app-private storage after install.
The local LLM is stored at:

```text
context.filesDir/models/local_llm/model.litertlm
```

Manifest example:

```json
{
  "models": [
    {
      "id": "local_llm_qwen2_5_1_5b",
      "version": "1.0.0",
      "kind": "LOCAL_LLM",
      "targetDirectoryName": "local_llm",
      "artifacts": [
        {
          "fileName": "model.litertlm",
          "url": "https://github.com/COR-VOX/CORVOX-Website/releases/download/models-v1/model.litertlm",
          "sha256": "replace_with_64_char_sha256",
          "sizeBytes": 1600000000
        }
      ]
    }
  ]
}
```

The manifest may contain only the `LOCAL_LLM` model. If KcELECTRA is absent, the
app remains usable and keeps the classifier on RuleBased fallback.

## Preparing `model.litertlm`

1. Convert or export the chosen on-device LLM into LiteRT-LM format as
   `model.litertlm`.
2. Calculate SHA-256 on Windows:

```powershell
Get-FileHash .\model.litertlm -Algorithm SHA256
```

3. Create a GitHub Release, for example `models-v1`.
4. Upload `model.litertlm` and `model_manifest.json` to that release.
5. Put the release asset URL for `model_manifest.json` into
   `MODEL_MANIFEST_URL`.

After download and SHA-256 verification, Local LLM fallback uses LiteRT-LM. The
client tries GPU first and falls back to CPU if GPU initialization fails.

## KcELECTRA Status

KcELECTRA is not active in the current build. Do not upload the original PyTorch
checkpoint and expect Android inference. To enable it later, prepare an
OpenGlow-task-specific fine-tuned TFLite package:

```text
model.tflite
vocab.txt
tokenizer_config.json
label_map.json
```

Until those files and the TFLite runtime path are wired, the setup screen shows:

```text
KcELECTRA는 아직 설정되지 않았습니다. 현재는 RuleBased 분류기를 사용합니다.
```

The app currently works as Gemini + Local LLM + RuleBased. If Gemini fails,
OpenGlow tries Local LLM when `ENABLE_LOCAL_LLM=true` and `model.litertlm` is
ready. If Local LLM also fails, it falls back to RuleBased analysis.

## Verification

Useful checks before release:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Inspect the generated APK and confirm it does not contain `libonnxruntime.so` or
`libonnxruntime4j_jni.so`. ONNX Runtime is intentionally not a dependency.

Functional checks:

1. Without `MODEL_MANIFEST_URL`, Local LLM shows `NotConfigured`.
2. With a manifest containing only `local_llm_qwen2_5_1_5b`, `model.litertlm`
   downloads and verifies.
3. After download, Local LLM availability becomes true when
   `ENABLE_LOCAL_LLM=true`.
4. KcELECTRA missing files must not crash the app.
5. Current classifier remains `RuleBased`.
6. Notification notes store the final summary only; unread/count notification
   noise is filtered.
7. Approved calendar suggestions are saved into the in-app calendar tab.
