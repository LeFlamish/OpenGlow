# OpenGlow

## Gemini local setup

Keep Gemini credentials in `local.properties`. This file is already ignored by Git.

```properties
sdk.dir=C\:\\Users\\jiuk0\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-3.5-flash
```

Do not commit a real API key. `app/build.gradle.kts` injects these values into
`BuildConfig.GEMINI_API_KEY` and `BuildConfig.GEMINI_MODEL` for local prototype
builds only.

## Test flow

1. Add `GEMINI_API_KEY` and `GEMINI_MODEL` to `local.properties`.
2. Build and run the app.
3. Grant notification listener access.
4. Receive a KakaoTalk personal message and confirm an individual note is created.
5. Receive a KakaoTalk group message and confirm a group note is created.
6. Receive SMS and email notifications and confirm SMS/EMAIL platform notes appear.
7. Send multiple notifications from the same person or group and confirm they accumulate in the same note.
8. Check the note card for summary, importance, work-related status, action items, deadline, and updated time.

## Logcat

Useful tags:

- `GeminiLlmClient`
- `NotificationRepo`
- `NoteDaoDebug`

The logs show model name, API-key presence, Gemini call lifecycle, parsing status,
and note create/update status. They must not print the full API key or full
notification body.

## Release note

The direct Gemini call is for prototype use only. Before release, route analysis
through a backend server because Android APKs can be inspected and any API key
embedded in `BuildConfig` can be extracted.

## On-device model downloads

OpenGlow does not bundle large model files inside the APK. The app is structured
to download model artifacts into internal app storage after installation:

- Local LLM: `context.filesDir/models/local_llm/model.litertlm`
- KcELECTRA classifier: `context.filesDir/models/kcelectra/model.onnx` or
  `model.tflite`, plus `vocab.txt`, `tokenizer_config.json` or `tokenizer.json`,
  and `label_map.json`

`ModelRegistry` owns model IDs, artifact names, download URLs, sizes, and
SHA-256 checksums. The current prototype leaves URLs and checksums as TODO
placeholders. Production can host the converted artifacts on Firebase Storage,
Cloudflare R2, S3, Hugging Face, GitHub Releases, or another CDN, then update
`ModelRegistry`.

KcELECTRA download support expects an Android-executable converted model. It
does not download the original Hugging Face PyTorch model and try to run it on
Android. The intended flow is:

1. Collect feedback data from the app.
2. Fine-tune KcELECTRA on a PC or server.
3. Convert the trained classifier to ONNX or TFLite.
4. Upload model, vocab, tokenizer config, label map, and SHA-256 checksums.
5. Update `ModelRegistry`.
6. Let the app download and verify the files.

The app does not perform real-time fine-tuning internally. It performs feedback
based personalization rules and, when available, on-device KcELECTRA inference.
When a model is missing, download fails, checksum verification fails, or runtime
loading fails, the app falls back to Gemini and rule-based analysis.
