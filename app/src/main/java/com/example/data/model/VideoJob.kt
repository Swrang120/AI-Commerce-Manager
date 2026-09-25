package com.example.data.model

data class VideoJob(
    val id: String,
    val productId: String?,
    val title: String,
    val description: String?,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val previewUrl: String?,
    val aspectRatio: String,
    val resolution: String,
    val durationSeconds: Int,
    val generationStatus: String,
    val generationProvider: String?,
    val generationPrompt: String?,
    val scheduledAt: String?,
    val generatedAt: String?,
    val publishedAt: String?,
    val downloadCount: Int,
    val viewCount: Int,
    val isPublic: Boolean,
    val errorMessage: String?
)