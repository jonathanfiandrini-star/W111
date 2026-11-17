package com.example.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CopilotHistoryTurn(
    val isUser: Boolean,
    val text: String
)

class CopilotRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Você é o assistente Copilot integrado ao Fluent 11 Launcher no Android. " +
                        "Responda em português de forma clara, objetiva e útil. " +
                        "Quando o usuário pedir sugestões de aplicativos, priorize citar apenas apps da lista instalada informada no contexto, quando fornecida."
                )
            }
        )
    }

    fun streamResponse(
        prompt: String,
        installedAppNames: List<String>,
        history: List<CopilotHistoryTurn>
    ): Flow<String> {
        val sdkHistory: List<Content> = history.takeLast(8).map { turn ->
            content(role = if (turn.isUser) "user" else "model") {
                text(turn.text)
            }
        }
        val contextualPrompt = if (installedAppNames.isNotEmpty() && history.isEmpty()) {
            val sampleApps = installedAppNames.take(30).joinToString(", ")
            "Aplicativos instalados no dispositivo: $sampleApps.\n\nPedido do usuário: $prompt"
        } else {
            prompt
        }
        val chat = model.startChat(history = sdkHistory)
        return chat.sendMessageStream(contextualPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
