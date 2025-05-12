package com.example.gradetracker

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.example.gradetracker.models.Subject
import com.example.gradetracker.utils.DialogHelper

// Data class for displaying goals
data class GoalDisplay(
    val subjectId: Long,
    val subjectName: String,
    val target: Double,
    val current: Double,
    val achieved: Boolean
)

class GoalActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvGoals: RecyclerView
    private lateinit var rvSubjects: RecyclerView
    private lateinit var goalAdapter: GoalRecyclerAdapter
    private lateinit var etTargetGrade: EditText
    private lateinit var btnSetGoal: Button
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var progressBar: ProgressBar
    private var subjectList: List<Subject> = listOf()
    private lateinit var subjectAdapter: SubjectRecyclerAdapter
    private var selectedSubject: Subject? = null

    companion object {
        private const val MIN_GRADE = 0.0
        private const val MAX_GRADE = 100.0
        private const val MIN_SUBJECT_LENGTH = 3
        private const val MAX_SUBJECT_LENGTH = 50
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)
        
        initializeViews()
        setupRecyclerViews()
        setupInputValidation()
        setupNavigation()
        loadData()
    }

    private fun initializeViews() {
        dbHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        
        rvGoals = findViewById(R.id.rvGoals)
        rvSubjects = findViewById(R.id.rvSubjects)
        etTargetGrade = findViewById(R.id.etTargetGrade)
        btnSetGoal = findViewById(R.id.btnSetGoal)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerViews() {
        // Setup Goals RecyclerView
        goalAdapter = GoalRecyclerAdapter(listOf(),
            onEditClick = { showEditGoalDialog(it) },
            onDeleteClick = { showDeleteGoalDialog(it) }
        )
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalAdapter
        
        // Setup Subjects RecyclerView
        subjectAdapter = SubjectRecyclerAdapter(subjectList) { subject ->
            selectedSubject = subject
            subjectAdapter.updateSubjects(subjectList.map { 
                if (it == subject) it.copy(name = "✓ " + it.name) else it 
            })
            validateInputs()
            hideKeyboard()
        }
        rvSubjects.layoutManager = LinearLayoutManager(this)
        rvSubjects.adapter = subjectAdapter
    }

    private fun setupInputValidation() {
        etTargetGrade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        btnSetGoal.setOnClickListener {
            if (validateInputs()) {
                hideKeyboard()
                setGoal()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val targetGrade = etTargetGrade.text.toString().toDoubleOrNull()
        val isValidGrade = targetGrade != null && targetGrade in MIN_GRADE..MAX_GRADE
        val isValidSubject = selectedSubject != null

        btnSetGoal.isEnabled = isValidGrade && isValidSubject

        if (!isValidGrade && etTargetGrade.text.isNotEmpty()) {
            etTargetGrade.error = "Grade must be between $MIN_GRADE and $MAX_GRADE"
        }

        if (!isValidSubject) {
            showSnackbar("Please select a subject")
        }

        return isValidGrade && isValidSubject
    }

    private fun loadData() {
        DialogHelper.showLoading(this, "Loading data...")
        try {
            loadSubjects()
            loadGoals()
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to load data: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun loadSubjects() {
        try {
            val cursor = dbHelper.getAllSubjects()
            val subjects = mutableListOf<Subject>()
            while (cursor.moveToNext()) {
                subjects.add(
                    Subject(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        term = cursor.getString(cursor.getColumnIndexOrThrow("term"))
                    )
                )
            }
            cursor.close()
            subjectList = subjects
            subjectAdapter.updateSubjects(subjectList)
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to load subjects: ${e.message}")
        }
    }

    private fun loadGoals() {
        try {
            val cursor = dbHelper.getAllGoalsWithSubjects()
            val goals = mutableListOf<GoalDisplay>()
            while (cursor.moveToNext()) {
                val subjectId = cursor.getLong(cursor.getColumnIndexOrThrow("subject_id"))
                val subjectName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val target = cursor.getDouble(cursor.getColumnIndexOrThrow("target_grade"))
                val avg = dbHelper.getAverageGradeForSubject(subjectId)
                goals.add(GoalDisplay(subjectId, subjectName, target, avg, avg >= target))
            }
            cursor.close()
            goalAdapter.updateGoals(goals)
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to load goals: ${e.message}")
        }
    }

    private fun setGoal() {
        val subject = selectedSubject ?: return
        val targetGrade = etTargetGrade.text.toString().toDoubleOrNull() ?: return

        DialogHelper.showLoading(this, "Setting goal...")
        try {
            dbHelper.setGoal(subject.id, targetGrade)
            DialogHelper.showSuccessDialog(this, "Goal set successfully!") {
                checkGoalAchieved(subject.id, subject.name, targetGrade)
                
                // Clear selection and input
                selectedSubject = null
                etTargetGrade.text.clear()
                loadSubjects()
                loadGoals()
            }
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to set goal: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun checkGoalAchieved(subjectId: Long, subjectName: String, targetGrade: Double) {
        val cursor = dbHelper.getGradesForSubject(subjectId)
        var currentGrade = 0.0
        var count = 0
        
        while (cursor.moveToNext()) {
            currentGrade += cursor.getDouble(cursor.getColumnIndexOrThrow("grade"))
            count++
        }
        cursor.close()
        
        if (count > 0) {
            currentGrade /= count
            if (currentGrade >= targetGrade) {
                notificationHelper.sendGoalAchievedNotification(subjectName, targetGrade)
            }
        }
    }

    private fun showEditGoalDialog(goal: GoalDisplay) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(goal.target.toString())
        }

        DialogHelper.showConfirmationDialog(
            context = this,
            title = "Edit Goal for ${goal.subjectName}",
            message = "Enter new target grade:",
            positiveText = "Save",
            negativeText = "Cancel"
        ) {
            val newTarget = input.text.toString().toDoubleOrNull()
            if (newTarget != null && newTarget in MIN_GRADE..MAX_GRADE) {
                DialogHelper.showLoading(this, "Updating goal...")
                try {
                    dbHelper.updateGoal(goal.subjectId, newTarget)
                    DialogHelper.showSuccessDialog(this, "Goal updated successfully!") {
                        loadGoals()
                    }
                } catch (e: Exception) {
                    DialogHelper.showErrorDialog(this, "Failed to update goal: ${e.message}")
                } finally {
                    DialogHelper.hideLoading()
                }
            } else {
                DialogHelper.showErrorDialog(this, "Please enter a valid grade between $MIN_GRADE and $MAX_GRADE")
            }
        }
    }

    private fun showDeleteGoalDialog(goal: GoalDisplay) {
        DialogHelper.showConfirmationDialog(
            context = this,
            title = "Delete Goal",
            message = "Are you sure you want to delete the goal for ${goal.subjectName}?",
            positiveText = "Delete",
            negativeText = "Cancel"
        ) {
            DialogHelper.showLoading(this, "Deleting goal...")
            try {
                dbHelper.deleteGoal(goal.subjectId)
                DialogHelper.showSuccessDialog(this, "Goal deleted successfully!") {
                    loadGoals()
                }
            } catch (e: Exception) {
                DialogHelper.showErrorDialog(this, "Failed to delete goal: ${e.message}")
            } finally {
                DialogHelper.hideLoading()
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_goals
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finishAffinity()
                    true
                }
                R.id.nav_subjects -> {
                    if (this !is SubjectManagementActivity) {
                        startActivity(Intent(this, SubjectManagementActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_goals -> true
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (selectedSubject != null) {
            selectedSubject = null
            loadSubjects()
        } else {
            super.onBackPressed()
        }
    }
} 