package com.example.gradetracker.utils

object ValidationHelper {
    const val MIN_GRADE = 0.0
    const val MAX_GRADE = 100.0
    const val MIN_SUBJECT_LENGTH = 3
    const val MAX_SUBJECT_LENGTH = 50
    const val MIN_TERM_LENGTH = 3
    const val MAX_TERM_LENGTH = 50

    fun validateGrade(grade: Double?): ValidationResult {
        return when {
            grade == null -> ValidationResult.Error("Grade cannot be empty")
            grade < MIN_GRADE -> ValidationResult.Error("Grade cannot be less than $MIN_GRADE")
            grade > MAX_GRADE -> ValidationResult.Error("Grade cannot be greater than $MAX_GRADE")
            else -> ValidationResult.Success
        }
    }

    fun validateGradeString(gradeStr: String): ValidationResult {
        val grade = gradeStr.toDoubleOrNull()
        return validateGrade(grade)
    }

    fun validateSubjectName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Subject name cannot be empty")
            name.length < MIN_SUBJECT_LENGTH -> ValidationResult.Error("Subject name must be at least $MIN_SUBJECT_LENGTH characters")
            name.length > MAX_SUBJECT_LENGTH -> ValidationResult.Error("Subject name cannot exceed $MAX_SUBJECT_LENGTH characters")
            else -> ValidationResult.Success
        }
    }

    fun validateTerm(term: String): ValidationResult {
        return when {
            term.isBlank() -> ValidationResult.Error("Term cannot be empty")
            term.length < MIN_TERM_LENGTH -> ValidationResult.Error("Term must be at least $MIN_TERM_LENGTH characters")
            term.length > MAX_TERM_LENGTH -> ValidationResult.Error("Term cannot exceed $MAX_TERM_LENGTH characters")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
} 