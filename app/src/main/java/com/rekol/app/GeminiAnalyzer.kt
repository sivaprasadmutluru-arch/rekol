package com.rekol.app

import android.graphics.BitmapFactory
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.FirebaseApp
import java.io.File

/** Generates a decision-focused explanation from text extracted from a capture. */
object GeminiAnalyzer {
    suspend fun analyze(extractedText: String, imageFile: File? = null): String {
        val text = extractedText.trim().take(18000)
        val model = FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI()).generativeModel("gemini-3.8-flash")
        val prompt = """
            You are Rekol, a practical memory assistant. Analyze the captured content below.
            Return concise plain text with exactly these headings:
            SUMMARY:
            IMPORTANT DETAILS:
            RISKS OR QUESTIONS:
            SUGGESTED ACTIONS:
            NEXT REMINDER:
            Do not invent facts. If a section is not supported by the content, write Not identified.
            Focus on decisions and useful next steps.

            CAPTURED CONTENT:
            $text
        """.trimIndent()
        val bitmap = imageFile?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
        val response = if (bitmap != null) {
            model.generateContent(Content("user", listOf(TextPart(prompt), ImagePart(bitmap))))
        } else if (text.isNotBlank()) {
            model.generateContent(prompt)
        } else {
            return "No readable text was found. Add a short description so Rekol can suggest next steps."
        }
        bitmap?.recycle()
        return response.text?.trim().orEmpty().ifBlank { "No analysis was returned." }
    }
}
