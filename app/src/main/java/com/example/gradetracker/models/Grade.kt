package com.example.gradetracker.models

data class Grade(
    val id: Long,
    val subjectId: Long,
    val grade: Double,
    val date: String
) 