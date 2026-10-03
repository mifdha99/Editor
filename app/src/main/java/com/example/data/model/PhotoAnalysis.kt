package com.example.data.model

data class PhotoAnalysis(
    val score: Float = 0.0f,
    val scoreSummary: String = "",
    val lighting: String = "",
    val composition: String = "",
    val mood: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val suggestedPrompts: List<String> = emptyList()
)
