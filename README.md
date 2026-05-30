# Multi-Model AI Chat 🚀

> An elegant, secure, offline-first client integrating multiple AI providers into a single, unified experience.

**Multi-Model AI Chat** is an Android application built with modern Kotlin and Jetpack Compose. It empowers users to seamlessly chat with multiple specialized AI models by bringing together leading platforms (Google Gemini, NVIDIA NIM, and OpenRouter) into a single, unified interface.

## ✨ Key Features

- **Unified Intelligence Gateway Context:** Access an evolving lineup of foundational and open models across Google Gemini models (e.g., Gemini 3.5 Flash), NVIDIA NIM endpoints, and OpenRouter architectures.
- **Secure On-Device Key Management & AES-128:** Your data is yours. All provided configuration parameters and AI Gateway API keys are encrypted at-rest using robust AES-128 local persistence right on your local device. We never harvest your credentials.
- **Clean MVVM Architecture & Room Persistance:** Messages are managed inside a responsive MVVM framework powered by `StateFlow` and cached locally across app restarts using the official Jetpack Room infrastructure. 
- **Modern Jetpack Compose UI:** A fluid, fully-adaptive Material 3 graphical interface focusing on optimal user experience and edge-to-edge typography, complete with multi-option dashboards.
- **Dynamic System Prompts:** Fully configurable prompt engineering system settings explicitly exposed to power-users inside the Settings dashboard.

## 🛠️ Architecture

This repository adopts modern Android application development standards:
- **Language**: Kotlin 
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Local Database**: Room DB (SQLite)
- **State Management**: Kotlin Coroutines & Flows + `ViewModel`
- **Network Processing**: Retrofit / OkHttp + Kotlinx Serialization
- **Security Frameworks**: Android `Base64` backed `Cipher` with `SecretKeySpec` AES processing modes

## 🔒 Security Summary
This application handles user-level external API keys. As per industry standards:
1. Keys are requested from the user within a secure local settings sheet.
2. We encrypt user settings models before pushing them down to the Room Database.
3. During request propagation within the internal `UnifiedAiGatewayService`, keys are decrypted exclusively in-memory, invoked in the Authorization header (`Bearer <KEY>` or query parameter binding for Gemini API), and are never persisted openly.

## 📦 Getting Started / Installation

1. **Clone the repository:**
   ```bash
   git clone <your-repo-link>
   cd multi-model-ai-chat
   ```
2. **Setup your environment:**
   Optionally open the local `.env` and fill default variables, though using the in-app client-side settings dashboard is fully supported.
3. **Build the Application:**
   Open the project using Android Studio Hedgehog (or later). Click the **Run** button to deploy the `assembleDebug` test variant locally to your device or emulator. 

## 🚀 Obtaining API Keys

The app behaves primarily as a Bring-Your-Own-Key (BYOK) system.
1. **Google Gemini:** Proceed to [Google AI Studio](https://aistudio.google.com/app/apikey) to generate an `AIza` key.
2. **NVIDIA Nim:** Generate an `nvapi-` key via the [NVIDIA API Dashboard](https://build.nvidia.com/).
3. **OpenRouter:** Generate an `sk-or-` generic key from OpenRouter settings.
All keys are managed purely via the in-app unified menu.

## 🤝 Contribution Guidelines
When iterating on the project:
- Verify standard testing paradigms (`gradle test`, `gradle :app:testDebugUnitTest`).
- Execute Jetpack Compose rendering evaluations by verifying layout boundary invariants inside `MainActivity.kt`.
- Preserve the MVVM data layers specifically inside the `/data/repository` and `/data/local` directories to maintain persistence hygiene. 

## 📄 License
This application is distributed under standard general open-use guidelines. Feel free to fork or submit PRs matching modern clean conventions.
