package com.example.gradetracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.gradetracker.models.Subject
import com.example.gradetracker.utils.DialogHelper
import com.example.gradetracker.utils.ValidationHelper
import com.example.gradetracker.utils.ValidationResult
import android.widget.LinearLayout

class SubjectManagementActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvSubjects: RecyclerView
    private lateinit var etSubjectName: EditText
    private lateinit var etSubjectTerm: EditText
    private lateinit var btnAddSubject: Button
    private lateinit var subjectAdapter: SubjectRecyclerAdapter
    private var subjectList: List<Subject> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_management)

        initializeViews()
        setupRecyclerViews()
        setupInputValidation()
        setupNavigation()
        loadSubjects()
    }

    private fun initializeViews() {
        dbHelper = DatabaseHelper(this)
        rvSubjects = findViewById(R.id.rvSubjects)
        etSubjectName = findViewById(R.id.etSubjectName)
        etSubjectTerm = findViewById(R.id.etSubjectTerm)
        btnAddSubject = findViewById(R.id.btnAddSubject)
    }

    private fun setupRecyclerViews() {
        subjectAdapter = SubjectRecyclerAdapter(subjectList) { subject ->
            showEditOptionsDialog(subject)
        }
        rvSubjects.layoutManager = LinearLayoutManager(this)
        rvSubjects.adapter = subjectAdapter
    }

    private fun setupInputValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        etSubjectName.addTextChangedListener(textWatcher)
        etSubjectTerm.addTextChangedListener(textWatcher)

        btnAddSubject.setOnClickListener {
            if (validateInputs()) {
                hideKeyboard()
                addSubject()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val subjectNameValidation = ValidationHelper.validateSubjectName(etSubjectName.text.toString())
        val termValidation = ValidationHelper.validateTerm(etSubjectTerm.text.toString())

        btnAddSubject.isEnabled = subjectNameValidation is ValidationResult.Success &&
                termValidation is ValidationResult.Success

        if (subjectNameValidation is ValidationResult.Error) {
            etSubjectName.error = subjectNameValidation.message
        }

        if (termValidation is ValidationResult.Error) {
            etSubjectTerm.error = termValidation.message
        }

        return subjectNameValidation is ValidationResult.Success &&
                termValidation is ValidationResult.Success
    }

    private fun loadSubjects() {
        DialogHelper.showLoading(this, "Loading subjects...")
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
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun addSubject() {
        val subjectName = etSubjectName.text.toString()
        val term = etSubjectTerm.text.toString()

        DialogHelper.showLoading(this, "Adding subject...")
        try {
            dbHelper.insertSubject(subjectName, term)
            etSubjectName.text.clear()
            etSubjectTerm.text.clear()
            loadSubjects()
            DialogHelper.showSuccessDialog(this, "Subject added successfully!")
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to add subject: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun showEditOptionsDialog(subject: Subject) {
        val options = arrayOf("Edit", "Delete", "Cancel")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Subject Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditDialog(subject)
                    1 -> showDeleteConfirmationDialog(subject)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showEditDialog(subject: Subject) {
        // Create a Material dialog with two TextInputEditTexts for name and term
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val nameInputLayout = com.google.android.material.textfield.TextInputLayout(context).apply {
            hint = "Subject Name"
        }
        val nameEdit = com.google.android.material.textfield.TextInputEditText(context).apply {
            setText(subject.name)
        }
        nameInputLayout.addView(nameEdit)
        val termInputLayout = com.google.android.material.textfield.TextInputLayout(context).apply {
            hint = "Term (e.g. Spring 2024)"
        }
        val termEdit = com.google.android.material.textfield.TextInputEditText(context).apply {
            setText(subject.term)
        }
        termInputLayout.addView(termEdit)
        layout.addView(nameInputLayout)
        layout.addView(termInputLayout)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Edit Subject")
            .setView(layout)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = nameEdit.text?.toString()?.trim() ?: ""
                val newTerm = termEdit.text?.toString()?.trim() ?: ""
                val nameValidation = ValidationHelper.validateSubjectName(newName)
                val termValidation = ValidationHelper.validateTerm(newTerm)
                if (nameValidation is ValidationResult.Error) {
                    nameInputLayout.error = nameValidation.message
                } else {
                    nameInputLayout.error = null
                }
                if (termValidation is ValidationResult.Error) {
                    termInputLayout.error = termValidation.message
                } else {
                    termInputLayout.error = null
                }
                if (nameValidation is ValidationResult.Success && termValidation is ValidationResult.Success) {
                    dbHelper.updateSubject(subject.copy(name = newName, term = newTerm))
                    loadSubjects()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showDeleteConfirmationDialog(subject: Subject) {
        DialogHelper.showConfirmationDialog(
            context = this,
            title = "Delete Subject",
            message = "Are you sure you want to delete ${subject.name}?",
            positiveText = "Delete",
            negativeText = "Cancel"
        ) {
            dbHelper.deleteSubject(subject.id)
            loadSubjects()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_subjects
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finishAffinity()
                    true
                }
                R.id.nav_subjects -> true
                R.id.nav_goals -> {
                    if (this !is GoalActivity) {
                        startActivity(Intent(this, GoalActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
