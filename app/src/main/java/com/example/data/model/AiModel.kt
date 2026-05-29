package com.example.data.model

data class AiModel(
    val id: String,
    val name: String,
    val platform: String, // "NVIDIA", "OpenRouter", "Both"
    val defaultNvidiaId: String = "",
    val defaultOpenRouterId: String = "",
    val category: String = "Normal",
    val description: String = ""
) {
    val computedCategory: String
        get() = when (id) {
            "nemotron_content_safety" -> "Safety"
            "nemotron_nano_12b_v2_vl", "streampetr", "bevformer", "phi_4_multimodal" -> "Vision"
            "nv_embed_code_7b", "qwen3_coder_480b" -> "Coding"
            "riva_translate", "sarvam_m" -> "Translation"
            "magpie_tts" -> "Voice"
            "esmfold", "lfm_2_5_1_2b_thinking", "nemotron_3_super_120b", "llama_4_maverick" -> "Reasoning"
            else -> if (category == "Normal") "General" else category
        }

    companion object {
        val FREE_MODELS = listOf(
            AiModel(
                "nemotron_mini_4b",
                "Nemotron Mini 4B",
                "NVIDIA",
                "nvidia/nemotron-mini-4b-instruct",
                category = "Normal",
                description = "NVIDIA's highly compact and efficient model for fast, standard responses."
            ),
            AiModel(
                "nemotron_content_safety",
                "Nemotron Content Safety",
                "NVIDIA",
                "nvidia/nemotron-content-safety",
                category = "Normal",
                description = "Specialized safety classifier to analyze content for policy compliance."
            ),
            AiModel(
                "nemotron_3_super_120b",
                "Nemotron 3 Super 120B",
                "Both",
                "nvidia/nemotron-3-super-120b",
                "nvidia/nemotron-3-super-120b",
                category = "Normal",
                description = "Premium high-capacity intelligence from NVIDIA optimized for reasoning."
            ),
            AiModel(
                "nemotron_3_nano_30b",
                "Nemotron 3 Nano 30B",
                "Both",
                "nvidia/nemotron-3-nano-30b",
                "nvidia/nemotron-3-nano-30b",
                category = "Normal",
                description = "A versatile balance of speed and power inside the Nemotron-3 family."
            ),
            AiModel(
                "nemotron_3_nano_omni_30b",
                "Nemotron 3 Nano Omni 30B",
                "Both",
                "nvidia/nemotron-3-nano-omni-30b",
                "nvidia/nemotron-3-nano-omni-30b",
                category = "Normal",
                description = "Natively cross-modal and multi-functional 30B parameter core."
            ),
            AiModel(
                "nemotron_nano_12b_v2_vl",
                "Nemotron Nano 12B v2 VL",
                "OpenRouter",
                defaultOpenRouterId = "nvidia/nemotron-nano-12b-v2-vl",
                category = "Normal",
                description = "Optimized for vision-language tasks including image analysis and scene parsing."
            ),
            AiModel(
                "nemotron_nano_9b_v2",
                "Nemotron Nano 9B v2",
                "OpenRouter",
                defaultOpenRouterId = "nvidia/nemotron-nano-9b-v2",
                category = "Normal",
                description = "Fast, efficient updated edge-capable NLP assistant on OpenRouter."
            ),
            AiModel(
                "nv_embed_v1",
                "NV-Embed V1",
                "NVIDIA",
                "nvidia/nv-embed-v1",
                category = "Normal",
                description = "Top-tier retrieval model designed for high-fidelity text embedding tasks."
            ),
            AiModel(
                "nv_embed_code_7b",
                "NV-EmbedCode 7B",
                "NVIDIA",
                "nvidia/nv-embedcode-7b",
                category = "Normal",
                description = "Specialized in understanding, summarizing, and writing clean programming code."
            ),
            AiModel(
                "riva_translate",
                "Riva Translate",
                "NVIDIA",
                "nvidia/riva-translate",
                category = "Normal",
                description = "Low-latency highly accurate machine translation across multiple languages."
            ),
            AiModel(
                "personaplex_7b",
                "PersonaPlex 7B",
                "NVIDIA",
                "nvidia/personaplex-7b",
                category = "Normal",
                description = "Roleplaying-tuned adapter optimized for distinct interactive character profiles."
            ),
            AiModel(
                "streampetr",
                "StreamPETR",
                "NVIDIA",
                "nvidia/streampetr",
                category = "Normal",
                description = "Advanced temporal multi-camera 3D object detection representation model."
            ),
            AiModel(
                "bevformer",
                "BEVFormer",
                "NVIDIA",
                "nvidia/bevformer",
                category = "Normal",
                description = "Spatiotemporal transformer for Bird's-Eye View perception pipelines."
            ),
            AiModel(
                "esmfold",
                "ESMFold",
                "NVIDIA",
                "nvidia/esmfold",
                category = "Normal",
                description = "Folding accelerator predicting accurate 3D protein structures from sequences."
            ),
            AiModel(
                "magpie_tts",
                "Magpie TTS",
                "NVIDIA",
                "nvidia/magpie-tts",
                category = "Normal",
                description = "Voice replication and text-to-speech formulation synthesizer."
            ),
            AiModel(
                "llama_4_maverick",
                "Llama 4 Maverick",
                "Both",
                "meta/llama-4-maverick",
                "meta/llama-4-maverick",
                category = "Normal",
                description = "An early look at experimental high-reasoning Llama-based architectures."
            ),
            AiModel(
                "llama_3_3_70b_instruct",
                "Llama 3.3 70B Instruct",
                "OpenRouter",
                defaultOpenRouterId = "meta-llama/llama-3.3-70b-instruct:free",
                category = "Normal",
                description = "Top-tier open intelligence from Meta, providing exceptional analytical outcomes."
            ),
            AiModel(
                "llama_3_2_3b_instruct",
                "Llama 3.2 3B Instruct",
                "OpenRouter",
                defaultOpenRouterId = "meta-llama/llama-3.2-3b-instruct:free",
                category = "Normal",
                description = "Extremely snappy and intelligent small-scale assistant perfect for chat."
            ),
            AiModel(
                "llama_3_1_8b",
                "Llama 3.1 8B",
                "NVIDIA",
                "meta/llama-3.1-8b-instruct",
                category = "Normal",
                description = "Meta's highly popular 8B instruction-tuned model running on NVIDIA infrastructure."
            ),
            AiModel(
                "hermes_3_llama_3_1_405b",
                "Hermes 3 Llama 3.1 405B",
                "OpenRouter",
                defaultOpenRouterId = "nousresearch/hermes-3-llama-3.1-405b:free",
                category = "Normal",
                description = "Flawless instruction-following and creative reasoning from Nous Research's 405B titan."
            ),
            AiModel(
                "mistral_large_3_675b",
                "Mistral Large 3 675B",
                "NVIDIA",
                "mistralai/mistral-large-3-675b",
                category = "Normal",
                description = "Flagship reasoning power from Mistral AI, optimized for complex logical tasks."
            ),
            AiModel(
                "mistral_nemotron",
                "Mistral Nemotron",
                "NVIDIA",
                "mistralai/mistral-nemotron-7b-instruct",
                category = "Normal",
                description = "The joint-optimized child of Mistral AI and NVIDIA computing."
            ),
            AiModel(
                "dolphin_mistral_24b",
                "Dolphin Mistral 24B",
                "OpenRouter",
                defaultOpenRouterId = "cognitivecomputations/dolphin-mixtral-8x22b",
                category = "Normal",
                description = "Famous Dolphin adapter known for unrestrained multi-turn complex answers."
            ),
            AiModel(
                "minimax_m2_7",
                "MiniMax M2.7",
                "NVIDIA",
                "minimax/m2.7",
                category = "Normal",
                description = "Optimized bilingual assistant focusing heavily on structured output."
            ),
            AiModel(
                "minimax_m2_5",
                "MiniMax M2.5",
                "OpenRouter",
                defaultOpenRouterId = "minimax/minimax-m2.5:free",
                category = "Normal",
                description = "Highly capable conversationalist with native Chinese/English performance."
            ),
            AiModel(
                "qwen3_coder_480b",
                "Qwen3 Coder 480B",
                "Both",
                "qwen/qwen3-coder-480b",
                "qwen/qwen3-coder-480b:free",
                category = "Normal",
                description = "Massive-scale premier developer model for bug finding and code generation."
            ),
            AiModel(
                "qwen3_5_122b",
                "Qwen3.5-122B",
                "NVIDIA",
                "qwen/qwen3.5-122b",
                category = "Normal",
                description = "High density 122B Qwen powerhouse with top-notch logical chains."
            ),
            AiModel(
                "qwen3_next_80b_a3b",
                "Qwen3 Next 80B A3B",
                "OpenRouter",
                defaultOpenRouterId = "qwen/qwen-3-next-80b",
                category = "Normal",
                description = "Next-generation experimental adapter with improved coherence protocols."
            ),
            AiModel(
                "deepseek_v3_2",
                "DeepSeek V3.2",
                "NVIDIA",
                "deepseek/deepseek-v3.2",
                category = "Normal",
                description = "DeepSeek's premium conversational intelligence with rapid inference on NIM."
            ),
            AiModel(
                "deepseek_v4_flash",
                "DeepSeek V4 Flash",
                "OpenRouter",
                defaultOpenRouterId = "deepseek/deepseek-v4-flash:free",
                category = "Normal",
                description = "High-velocity ultra-optimized reasoning model from DeepSeek on OpenRouter."
            ),
            AiModel(
                "gemma_3n_e2b",
                "Gemma 3n E2B",
                "NVIDIA",
                "google/gemma-3n-e2b",
                category = "Normal",
                description = "Google's ultra-compact, hyper-efficient Gemma 3 preview model hosted on NVIDIA."
            ),
            AiModel(
                "gemma_3n_e4b",
                "Gemma 3n E4B",
                "NVIDIA",
                "google/gemma-3n-e4b",
                category = "Normal",
                description = "Expanded 4B Google Gemma 3 preview with robust structural accuracy."
            ),
            AiModel(
                "gemma_4_31b",
                "Gemma 4 31B",
                "OpenRouter",
                defaultOpenRouterId = "google/gemma-2-9b-it:free",
                category = "Normal",
                description = "Google's next-level open source model delivering elite conceptual answers."
            ),
            AiModel(
                "gemma_4_26b_a4b",
                "Gemma 4 26B A4B",
                "OpenRouter",
                defaultOpenRouterId = "google/gemma-4-26b",
                category = "Normal",
                description = "Optimized 26B Gemma structure balancing high intellect and rapid response."
            ),
            AiModel(
                "lyria_3_pro_preview",
                "Lyria 3 Pro Preview",
                "OpenRouter",
                defaultOpenRouterId = "google/lyria-3-pro",
                category = "Normal",
                description = "Advanced prompt formulation helper designed to stimulate rich storytelling."
            ),
            AiModel(
                "lyria_3_clip_preview",
                "Lyria 3 Clip Preview",
                "OpenRouter",
                defaultOpenRouterId = "google/lyria-3-clip",
                category = "Normal",
                description = "Short-form content generation and engaging interactive copy planner."
            ),
            AiModel(
                "phi_4_multimodal",
                "Phi-4 Multimodal",
                "NVIDIA",
                "microsoft/phi-4-multimodal",
                category = "Normal",
                description = "Microsoft's latest multi-modal Phi-4 release supporting compound intelligence."
            ),
            AiModel(
                "gpt_oss_120b",
                "GPT-OSS 120B",
                "OpenRouter",
                defaultOpenRouterId = "gpt-oss/120b",
                category = "Normal",
                description = "Fully open source 120B model tuned for ultimate scientific explanation."
            ),
            AiModel(
                "gpt_oss_20b",
                "GPT-OSS 20B",
                "OpenRouter",
                defaultOpenRouterId = "gpt-oss/20b",
                category = "Normal",
                description = "Balanced 20B parameters optimized for robust, low-latency daily tasks."
            ),
            AiModel(
                "step_3_5_flash",
                "Step-3.5 Flash",
                "NVIDIA",
                "step/step-3.5-flash",
                category = "Normal",
                description = "Sub-second inference times specializing in parsing and high volume chat."
            ),
            AiModel(
                "glm_5_1",
                "GLM-5.1",
                "NVIDIA",
                "thm/glm-5.1",
                category = "Normal",
                description = "Expert reasoning and logic model for coding, hardware systems, and mathematics."
            ),
            AiModel(
                "glm_4",
                "GLM-4",
                "NVIDIA",
                "thm/glm-4",
                category = "Normal",
                description = "Tsinghua University's generalized base assistant optimized on NVIDIA NIM."
            ),
            AiModel(
                "glm_4_5_air",
                "GLM-4.5 Air",
                "OpenRouter",
                defaultOpenRouterId = "thm/glm-4-9b-chat:free",
                category = "Normal",
                description = "Very fast, highly logical Chat model tailored for quick interactions."
            ),
            AiModel(
                "kimi_k2_5",
                "Kimi K2.5",
                "NVIDIA",
                "kimi/k2.5",
                category = "Normal",
                description = "Moonshot Kimi AI model with stellar search summaries and long-context capabilities."
            ),
            AiModel(
                "kimi_k2_6",
                "Kimi K2.6",
                "OpenRouter",
                defaultOpenRouterId = "kimi/k2.6",
                category = "Normal",
                description = "Updated, extra-efficient conversationalist with long context windows."
            ),
            AiModel(
                "seed_oss_36b",
                "Seed-OSS 36B",
                "NVIDIA",
                "seed/seed-oss-36b",
                category = "Normal",
                description = "Advanced open research model focused on scientific and logical problem-solving."
            ),
            AiModel(
                "sarvam_m",
                "Sarvam-M",
                "NVIDIA",
                "sarvam/sarvam-m",
                category = "Normal",
                description = "Indic-tuned model prioritizing regional linguistic fluency and translations."
            ),
            AiModel(
                "lfm_2_5_1_2b_thinking",
                "LFM-2.5 1.2B Thinking",
                "OpenRouter",
                defaultOpenRouterId = "liquid/lfm-2.5-1.2b-thinking:free",
                category = "Normal",
                description = "Liquid's revolutionary thinking model carrying out complex mental reasoning steps."
            ),
            AiModel(
                "lfm_2_5_1_2b_instruct",
                "LFM-2.5 1.2B Instruct",
                "OpenRouter",
                defaultOpenRouterId = "liquid/lfm-2.5-1.2b-instruct:free",
                category = "Normal",
                description = "Extremely snappy non-thinking instruct variant from Liquid Foundation Models."
            ),
            AiModel(
                "laguna_xs_2",
                "Laguna XS.2",
                "OpenRouter",
                defaultOpenRouterId = "laguna/xs.2",
                category = "Normal",
                description = "Super fast lightweight assistant from Laguna labs tailored for conversational chat."
            ),
            AiModel(
                "laguna_m_1",
                "Laguna M.1",
                "OpenRouter",
                defaultOpenRouterId = "laguna/m.1",
                category = "Normal",
                description = "Medium-duty general purpose model supporting dense task chains."
            ),
            AiModel(
                "owl_alpha",
                "Owl Alpha",
                "OpenRouter",
                defaultOpenRouterId = "owl/alpha",
                category = "Normal",
                description = "Experimental conceptual writing and narrative brainstorming engine."
            ),
            AiModel(
                "openrouter_free_auto",
                "openrouter/free (auto-router)",
                "OpenRouter",
                defaultOpenRouterId = "openrouter/auto-router",
                category = "Normal",
                description = "OpenRouter's dynamic router that forwards your request to the best online free model."
            )
        )
    }
}
