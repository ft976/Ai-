# Multi-Model AI Chat 🚀

> An elegant, secure, offline-first client integrating multiple AI providers into a single, unified experience.

**Multi-Model AI Chat** is a state-of-the-art Android application built with modern Kotlin and Jetpack Compose. It empowers users to seamlessly chat with specialized AI models by bringing together leading platforms (Google Gemini, NVIDIA NIM) into a single, unified interface.

## ✨ Key Features & Comprehensive Logic

- **Unified Intelligence Gateway Context:** Access an evolving lineup of foundational and open models across Google Gemini models (Gemini 1.5 Pro, Flash) and NVIDIA NIM endpoints (Qwen, DeepSeek, Mistral, Llama, Nemotron).
- **Secure On-Device Key Management & AES-128:** Your data is yours. All provided configuration parameters and AI Gateway API keys are encrypted at-rest using robust AES-128 local persistence right on your local device. We never harvest your credentials.
- **Clean MVVM Architecture & Room Persistance:** Messages are managed inside a responsive MVVM framework powered by `StateFlow` and cached locally across app restarts using the official Jetpack Room infrastructure. 
- **Modern Jetpack Compose UI:** A fluid, fully-adaptive Material 3 graphical interface focusing on optimal user experience and edge-to-edge typography, complete with dynamic layouts.

## 🛠️ Architecture Deep-Dive

This repository adopts modern Android application development standards:
- **Language**: Kotlin 
- **UI Toolkit**: Jetpack Compose (Material 3) with WindowInsets handling for edge-to-edge layouts.
- **Local Database**: Room DB (SQLite) featuring entities like `ChatMessageModel` and `ChatSessionModel` with auto-migration and fast SSD serialization.
- **State Management**: Kotlin Coroutines & Flows. The central brain is `ChatViewModel`, processing UI mutations safely across threads and providing pure Flow streams to Composable elements.
- **Network Processing**: Retrofit / OkHttp + Kotlinx Serialization. The `UnifiedAiGatewayService` is a robust client routing interface scaling securely to endpoint constraints.
- **Security Frameworks**: Android `Base64` backed `Cipher` with `SecretKeySpec`. The entire payload is routed cryptographically avoiding cleartext persistence.

## 🗄️ Local Room Database Architecture

The core relational mapper comprises two entities:
1. `ChatSessionModel`: Defines the scope of the interaction, the UI title, and the designated `modelId` enforcing contextual bounds.
2. `ChatMessageModel`: Extends the relational map associating directly with a session UID. Captures the timestamp, exact response token text, the originating actor (User vs. System), and platform origin (NVIDIA vs. Gemini).

Both entities are manipulated through the DAOs using suspend functions triggered exclusively via Repository boundaries to ensure Thread-safe Room transactions.

## 🔒 Security & AES-128 Protocols

This application handles user-level external API keys. As per industry standards:
1. **User Key Input**: Keys are requested from the user within a secure local settings sheet.
2. **At-rest SQLite Cryptography**: We encrypt user settings models before pushing them down to the Room Database. Using `javax.crypto.Cipher`, we invoke AES/ECB/PKCS5Padding transformation profiles relying on an obfuscated pre-shared schema.
3. **In-Flight Ephemeral Execution**: During request propagation within the internal `UnifiedAiGatewayService`, keys are decrypted exclusively in-memory, invoked in the Authorization header (`Bearer <KEY>` or query parameter binding for Gemini API), and are never persisted openly.
4. **No Telemetry**: No tracking SDKs or background services process the LLM generated bodies or prompt definitions.

## 📦 SDK Interactions

- **Google AI Studio (Gemini)**: Routes REST bodies to `generativelanguage.googleapis.com`. It relies heavily on strict Content schemas representing parts and roles structure required by the Gemini payload specification.
- **NVIDIA NIM API**: Simulates OpenAI-compatible structured payloads (`/v1/chat/completions`) routing complex cross-linked requests safely mapping multi-modal representations efficiently.

## 🚀 Obtaining API Keys

The app behaves primarily as a Bring-Your-Own-Key (BYOK) system.
1. **Google Gemini:** Proceed to [Google AI Studio](https://aistudio.google.com/app/apikey) to generate an `AIza` key.
2. **NVIDIA Nim:** Generate an `nvapi-` key via the [NVIDIA API Dashboard](https://build.nvidia.com/).
All keys are managed purely via the in-app unified menu.

## 🤝 Contribution Guidelines
When iterating on the project:
- Verify standard testing paradigms (`gradle test`, `gradle :app:testDebugUnitTest`).
- Preserve the MVVM data layers specifically inside the `/data/repository` and `/data/local` directories to maintain persistence hygiene. 

## 📄 License
This application is distributed under standard general open-use guidelines. Feel free to fork or submit PRs matching modern clean conventions.
