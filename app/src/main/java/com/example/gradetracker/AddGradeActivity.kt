package com.example.gradetracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.example.gradetracker.models.Subject
import com.example.gradetracker.utils.DialogHelper
import com.example.gradetracker.utils.ValidationHelper
import com.example.gradetracker.utils.ValidationResult
import java.text.SimpleDateFormat
import java.util.*
import android.view.ViewGroup
import android.widget.LinearLayout
import android.view.View
import android.util.TypedValue

class AddGradeActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvSubjects: RecyclerView
    private lateinit var spinnerGradeType: android.widget.Spinner
    private lateinit var btnAddGradeType: Button
    private lateinit var fabAddGrade: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var etGrade: com.google.android.material.textfield.TextInputEditText
    private lateinit var gradeInputLayout: com.google.android.material.textfield.TextInputLayout
    private var subjectList: List<Subject> = listOf()
    private val LOW_GRADE_THRESHOLD = 60.0
    private lateinit var subjectAdapter: SubjectRecyclerAdapter
    private var selectedSubject: Subject? = null
    private var selectedGradeType: String = "Prelim"
    private var enteredTypes: MutableSet<String> = mutableSetOf()
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_detail)
        
        initializeViews()
        setupRecyclerViews()
        setupInputValidation()
        setupNavigation()
        loadSubjects()
    }

    private fun initializeViews() {
        dbHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        rvSubjects = findViewById(R.id.rvGrades)
        spinnerGradeType = findViewById(R.id.spinnerGradeType)
        btnAddGradeType = findViewById(R.id.btnAddGradeType)
        fabAddGrade = findViewById(R.id.fabAddGrade)

        // Create grade input field
        gradeInputLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
            hint = "Enter Grade"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val margin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,
                resources.displayMetrics
            ).toInt()
            setPadding(margin, margin, margin, margin)
        }
        etGrade = com.google.android.material.textfield.TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        gradeInputLayout.addView(etGrade)

        // Add grade input to layout after spinner
        val parentLayout = findViewById<ViewGroup>(R.id.parentLayout)
        val spinnerView = findViewById<View>(R.id.spinnerGradeType)
        val spinnerIndex = parentLayout.indexOfChild(spinnerView)
        parentLayout.addView(gradeInputLayout, spinnerIndex + 1)
    }

    private fun setupRecyclerViews() {
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
        etGrade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        spinnerGradeType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedGradeType = spinnerGradeType.selectedItem.toString()
                validateInputs()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        btnAddGradeType.setOnClickListener {
            if (validateInputs()) {
                hideKeyboard()
                saveGrade()
            }
        }

        fabAddGrade.setOnClickListener {
            if (validateInputs()) {
                hideKeyboard()
                saveGrade()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val gradeValidation = ValidationHelper.validateGradeString(etGrade.text.toString())
        val isValidSubject = selectedSubject != null
        val isTypeAvailable = !enteredTypes.contains(selectedGradeType)

        btnAddGradeType.isEnabled = gradeValidation is ValidationResult.Success && isValidSubject && isTypeAvailable
        fabAddGrade.isEnabled = btnAddGradeType.isEnabled

        if (gradeValidation is ValidationResult.Error) {
            gradeInputLayout.error = gradeValidation.message
        } else {
            gradeInputLayout.error = null
        }

        if (!isValidSubject) {
            Snackbar.make(etGrade, "Please select a subject", Snackbar.LENGTH_SHORT).show()
        }
        if (!isTypeAvailable) {
            btnAddGradeType.text = "Grade for $selectedGradeType already exists"
        } else {
            btnAddGradeType.text = "Add Grade for Selected Type"
        }
        return gradeValidation is ValidationResult.Success && isValidSubject && isTypeAvailable
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
            enteredTypes.clear()
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to load subjects: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun saveGrade() {
        val subject = selectedSubject ?: return
        val gradeStr = etGrade.text.toString()
        val gradeValidation = ValidationHelper.validateGradeString(gradeStr)
        if (gradeValidation is ValidationResult.Error) {
            DialogHelper.showErrorDialog(this, gradeValidation.message)
            return
        }
        val grade = gradeStr.toDouble()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val type = selectedGradeType
        DialogHelper.showLoading(this, "Saving grade...")
        try {
            dbHelper.insertGrade(subject.id, grade, date, type)
            enteredTypes.add(type)
            if (grade < LOW_GRADE_THRESHOLD) {
                notificationHelper.sendLowGradeNotification(subject.name, grade)
            }
            DialogHelper.showSuccessDialog(this, "Grade saved successfully!") {
                setResult(Activity.RESULT_OK)
                finish()
            }
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to save grade: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
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

    override fun onBackPressed() {
        if (selectedSubject != null) {
            selectedSubject = null
            loadSubjects()
        } else {
            super.onBackPressed()
        }
    }
} 