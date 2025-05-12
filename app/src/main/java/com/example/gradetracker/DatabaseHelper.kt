package com.example.gradetracker

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import com.example.gradetracker.models.Subject

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "GradeTracker.db"
        private const val DATABASE_VERSION = 2 // Incremented for migration
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE subjects(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, term TEXT)")
        db.execSQL("CREATE TABLE grades(id INTEGER PRIMARY KEY AUTOINCREMENT, subject_id INTEGER, grade REAL, date TEXT, type TEXT, FOREIGN KEY(subject_id) REFERENCES subjects(id))")
        db.execSQL("CREATE TABLE goals(id INTEGER PRIMARY KEY AUTOINCREMENT, subject_id INTEGER, target_grade REAL, FOREIGN KEY(subject_id) REFERENCES subjects(id))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add 'type' column to grades table if upgrading from version 1
            db.execSQL("ALTER TABLE grades ADD COLUMN type TEXT DEFAULT 'Prelim'")
        }
        db.execSQL("DROP TABLE IF EXISTS grades")
        db.execSQL("DROP TABLE IF EXISTS subjects")
        db.execSQL("DROP TABLE IF EXISTS goals")
        onCreate(db)
    }

    // Subject CRUD
    fun insertSubject(name: String, term: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("term", term)
        }
        db.insert("subjects", null, values)
    }

    fun getAllSubjects(): Cursor = readableDatabase.rawQuery("SELECT * FROM subjects", null)

    // Grade CRUD
    fun insertGrade(subjectId: Long, grade: Double, date: String, type: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("subject_id", subjectId)
            put("grade", grade)
            put("date", date)
            put("type", type)
        }
        return db.insert("grades", null, values)
    }

    fun getGradesForSubject(subjectId: Long): Cursor = readableDatabase.rawQuery(
        "SELECT * FROM grades WHERE subject_id = ? ORDER BY date ASC", arrayOf(subjectId.toString())
    )

    fun getAllGrades(): Cursor = readableDatabase.rawQuery("SELECT grade FROM grades", null)

    fun updateGrade(gradeId: Long, newGrade: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("grade", newGrade)
        }
        db.update("grades", values, "id = ?", arrayOf(gradeId.toString()))
    }

    // Goal CRUD
    fun setGoal(subjectId: Long, targetGrade: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("subject_id", subjectId)
            put("target_grade", targetGrade)
        }
        db.insertWithOnConflict("goals", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getGoalForSubject(subjectId: Long): Cursor = readableDatabase.rawQuery(
        "SELECT target_grade FROM goals WHERE subject_id = ?", arrayOf(subjectId.toString())
    )

    // Get all goals with subject names
    fun getAllGoalsWithSubjects(): Cursor = readableDatabase.rawQuery(
        """
        SELECT goals.subject_id, goals.target_grade, subjects.name
        FROM goals
        JOIN subjects ON goals.subject_id = subjects.id
        """, null
    )

    // Get average grade for a subject
    fun getAverageGradeForSubject(subjectId: Long): Double {
        val cursor = readableDatabase.rawQuery(
            "SELECT AVG(grade) as avg_grade FROM grades WHERE subject_id = ?", arrayOf(subjectId.toString())
        )
        val avg = if (cursor.moveToFirst()) cursor.getDouble(cursor.getColumnIndexOrThrow("avg_grade")) else 0.0
        cursor.close()
        return avg
    }

    // Delete a goal
    fun deleteGoal(subjectId: Long) {
        val db = writableDatabase
        db.delete("goals", "subject_id = ?", arrayOf(subjectId.toString()))
    }

    // Update a goal
    fun updateGoal(subjectId: Long, newTarget: Double) {
        val db = writableDatabase
        val values = ContentValues().apply { put("target_grade", newTarget) }
        db.update("goals", values, "subject_id = ?", arrayOf(subjectId.toString()))
    }

    fun deleteGrade(gradeId: Long) {
        val db = writableDatabase
        db.delete("grades", "id = ?", arrayOf(gradeId.toString()))
    }

    // Update a subject
    fun updateSubject(subject: Subject): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("name", subject.name)
            put("term", subject.term)
        }
        return db.update(
            "subjects",
            values,
            "id = ?",
            arrayOf(subject.id.toString())
        )
    }

    // Delete a subject
    fun deleteSubject(subjectId: Long): Int {
        val db = this.writableDatabase
        return db.delete(
            "subjects",
            "id = ?",
            arrayOf(subjectId.toString())
        )
    }

} 