# VoFlow

VoFlow is an Android voice-to-text app built around fast, system-wide dictation.

It combines a floating microphone control, Android's microphone foreground-service APIs, an accessibility service for text injection, and Google's Gemini API for AI-powered transcription.

## Features

- System-wide voice dictation from a floating microphone button.
- AI transcription through Gemini models.
- Smart dictation mode that cleans up filler words, false starts, punctuation, and obvious transcription mistakes while preserving the speaker's intent.
- Verbatim transcription mode when the original wording should be preserved.
- Automatic injection of transcribed text into supported focused text fields.
- Clipboard fallback when direct text injection is unavailable.
- Configurable Gemini model selection.
- Secure local storage for sensitive preferences and API credentials.
- Technical diagnostics and transcription/error logging.
- Shake-to-restore support for the floating microphone control.
- Minimal, local-first architecture with no analytics or telemetry.

## How It Works

1. The user starts a recording from the floating microphone control.
2. VoFlow captures audio as an AAC/M4A recording.
3. The recording is sent to the selected Gemini transcription model.
4. Gemini returns the transcription.
5. VoFlow injects the result into the currently focused text field when possible.
6. If direct injection is unavailable, the text can fall back to the clipboard.

The app keeps the recording pipeline intentionally small: audio capture is handled by Android's `MediaRecorder`, while network communication uses OkHttp.

## Requirements

- Android 7.0 (API 24) or newer.
- Microphone permission.
- Display-over-other-apps permission for the floating microphone.
- Accessibility-service permission for system-wide text targeting and injection.
- Notification permission where required by the Android version.
- A Gemini API key for AI transcription.

## Permissions

VoFlow requests the following Android capabilities:

| Permission | Purpose |
| --- | --- |
| `RECORD_AUDIO` | Capture voice input |
| `SYSTEM_ALERT_WINDOW` | Display the floating microphone control |
| `FOREGROUND_SERVICE` | Keep the recording service running correctly |
| `FOREGROUND_SERVICE_MICROPHONE` | Declare microphone foreground-service use |
| `BIND_ACCESSIBILITY_SERVICE` | Detect focused text fields and inject transcription |
| `INTERNET` | Communicate with Gemini |
| `ACCESS_NETWORK_STATE` | Check network state |
| `VIBRATE` | Haptic feedback |
| `POST_NOTIFICATIONS` | Foreground-service notifications on supported Android versions |

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX
- Kotlin Coroutines
- OkHttp
- Android MediaRecorder
- Android Accessibility Service
- Android foreground services
- Gemini API

The project intentionally avoids unnecessary frameworks and dependencies.

## Project Structure

```text
app/
└── src/main/
    ├── java/com/example/
    │   ├── data/
    │   │   ├── GeminiApiClient.kt
    │   │   ├── DiagnosticLog.kt
    │   │   └── ...
    │   ├── audio/
    │   │   └── AudioCaptureEngine.kt
    │   ├── service/
    │   │   ├── OverlayService.kt
    │   │   └── AuraAccessibilityService.kt
    │   └── ...
    ├── res/
    └── AndroidManifest.xml
```

The source namespace remains `com.example` for compatibility with the current project structure. The installed application ID is `com.jbuilds.voflow`.

## Building

Open the project in Android Studio with a recent Android SDK and JDK 17 installation.

The project currently uses:

- Android Gradle Plugin 9.1.1
- Kotlin 2.2.10
- Compile SDK 36.1
- Target SDK 36
- Minimum SDK 24
- Java 17

For a local debug build:

```bash
./gradlew assembleDebug
```

For a release build:

```bash
./gradlew assembleRelease
```

Release builds use R8 code shrinking and resource shrinking and produce ABI-specific APKs for:

- arm64-v8a
- armeabi-v7a
- x86_64

A universal APK is also configured.

## Release Signing

Release signing is configured through environment variables rather than storing signing credentials in the repository.

Expected variables:

```text
KEYSTORE_PATH
STORE_PASSWORD
KEY_PASSWORD
```

The release key alias is `upload`.

Do not commit keystores, passwords, API keys, or other credentials.

## Privacy

VoFlow is designed to keep application data local wherever possible.

- API credentials are stored using Android secure preferences.
- Diagnostic logs are kept locally in memory.
- API keys, audio recordings, transcripts, and request bodies are not written to the diagnostic log.
- No analytics or tracking system is included.
- Audio is captured locally and sent to Gemini only when transcription is requested.

Because VoFlow uses Gemini for AI transcription, recorded audio is transmitted to the configured Gemini API endpoint when a transcription request is made. Review Google's applicable Gemini API terms and privacy documentation before deploying the app for sensitive workloads.

## Diagnostics

VoFlow includes a Technical Logs section for troubleshooting transcription and application behavior.

Current diagnostics include events such as:

- Transcription start.
- Selected Gemini model.
- Quota/cooldown conditions.
- Empty/no-speech results.
- Transcription failures.
- Runtime exceptions.

The diagnostic system is intentionally designed to avoid logging sensitive content.

## Architecture Principles

The project follows a few simple rules:

1. Prefer Android and browser-native/system APIs over additional dependencies.
2. Keep the recording path lightweight.
3. Keep network work off the main thread.
4. Store sensitive configuration securely.
5. Request only permissions required for core functionality.
6. Avoid telemetry and unnecessary network activity.
7. Make changes surgical and preserve existing behavior.

## Status

**Version:** 1.0.0  
**Application ID:** `com.jbuilds.voflow`

VoFlow is under active development.

---

## License

VoFlow is licensed under the **MIT License**.

See [LICENSE](LICENSE) for the full license text.

---

> [!NOTE]
> The core logic and application code were generated with **AI** under my supervision and direction.
