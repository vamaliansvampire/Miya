# Miya AI (Jarvix Edition)

An intelligent, conversational, and futuristic Android personal assistant inspired by J.A.R.V.I.S., powered by the **Google Gemini API**.

---

## Key Features

### 1. Jarvis-Style Core Intelligence
- **Natural Multi-Turn Conversations**: Context-aware dialogue with multi-turn memory.
- **Configurable Personas**:
  - *Jarvis AI*: Ultra-competent, loyal, technical wit.
  - *Executive Chief of Staff*: Highly organized, strategic, decision-oriented.
  - *Senior Architect & Coder*: Deep STEM, Kotlin/Android architecture.
  - *Friendly Companion*: Warm, empathetic conversationalist.
- **Multilingual Support**: English, Urdu, and Roman Urdu.
- **Markdown & Code Rendering**: Formatted text with syntax code blocks and 1-tap copy.
- **Response Controls**: Stop generation, regenerate responses, copy, and share.

### 2. Mandatory Gemini API Key Management
- **Hardware-Backed Security**: Encrypted locally using **Android KeyStore (AES-GCM 256-bit)**.
- **Interactive Settings Screen**:
  - Enter, edit, and replace API keys.
  - Show/hide key visibility toggle.
  - Real-time connection ping test with latency metrics.
  - Delete/clear keys securely.
  - Choose between Gemini models (`gemini-3.5-flash`, `gemini-3.1-pro-preview`, `gemini-2.5-flash-image`).
  - Direct guide link to [Google AI Studio](https://aistudio.google.com/apikey).

### 3. Voice Assistant
- **Speech-to-Text**: Voice input via Android `SpeechRecognizer` with real-time audio waveform visualizer.
- **Text-to-Speech**: Natural voice synthesis with configurable speech rate and pitch.
- **Bilingual Recognition**: English and Urdu voice input selection.

### 4. Device Automation & Intent Integration
- **App Launching**: Open YouTube, Google Maps, Camera, Calculator, Settings, and Calendar.
- **Alarms & Timers**: Set device alarms and countdown timers via Android system APIs.
- **Web Search**: 1-tap Google web search queries.
- **Device Telemetry**: Real-time battery status, charging source, network connectivity, and storage stats.

### 5. Productivity (Tasks & Notes)
- **Local Room Database**:
  - *Tasks*: Interactive to-do list with priority badges (High, Medium, Low).
  - *Notes*: Categorized notes and saved AI insights with search filters.
  - *Chat History*: Multi-session conversation management.

---

## Technical Architecture

```
com.example
├── core
│   ├── security (Android KeyStore AES-GCM Encryption)
│   └── settings (Model, Personas, Voice & Appearance Preferences)
├── data
│   ├── local (Room DB: Sessions, Messages, Tasks, Notes)
│   ├── remote (Gemini REST Client, Streaming SSE Parser, Models)
│   └── repository (MiyaRepository)
├── domain
│   ├── actions (DeviceActionHandler: Intents, Alarms, Telemetry)
│   └── voice (VoiceAssistantManager: STT & TTS Engine)
└── ui
    ├── chat (ChatScreen & ChatViewModel)
    ├── components (JarvisCoreHud, MarkdownText, ActionCard)
    ├── history (HistoryScreen)
    ├── settings (SettingsScreen & SettingsViewModel)
    ├── tasks (TasksNotesScreen & TasksNotesViewModel)
    ├── theme (Color, Theme, Typography)
    └── tools (DeviceToolsScreen & DeviceToolsViewModel)
```

---

## Build & Run Instructions

### Prerequisites
- Android Studio Ladybug or later / AGP 8.8+
- JDK 17 or JDK 21
- Android SDK 36 (minSdk 24)

### Building the APK
Run Gradle to assemble the debug APK:
```bash
gradle assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

---

## License & Attribution
Powered by Google Gemini Models (`gemini-3.5-flash`). Built with Jetpack Compose & Material 3.
