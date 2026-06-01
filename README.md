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
