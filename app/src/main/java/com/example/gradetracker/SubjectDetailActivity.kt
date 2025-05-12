package com.example.gradetracker

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.gradetracker.models.Grade
import com.example.gradetracker.utils.DialogHelper
import com.example.gradetracker.utils.ValidationHelper
import com.example.gradetracker.utils.ValidationResult
import java.util.*
import android.widget.Button
import android.widget.Spinner
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import android.text.TextWatcher
import android.text.Editable
import java.text.SimpleDateFormat

class SubjectDetailActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvGrades: RecyclerView
    private lateinit var chart: LineChart
    private lateinit var fabAddGrade: FloatingActionButton
    private lateinit var btnAddGradeType: Button
    private lateinit var spinnerGradeType: Spinner
    private lateinit var gradeInputLayout: TextInputLayout
    private lateinit var etGrade: TextInputEditText
    private lateinit var gradeAdapter: GradeRecyclerAdapter
    private var gradeList: List<Grade> = listOf()
    private var subjectId: Long = -1
    private var subjectName: String = ""
    private var subjectTerm: String = ""
    private var gradeDisplayList: List<GradeRecyclerAdapter.GradeDisplay> = listOf()
    private var selectedGradeType: String = "Prelim"
    private var enteredTypes: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_detail)
        
        initializeViews()
        setupRecyclerViews()
        setupChart()
        setupInputValidation()
        setupNavigation()
        loadData()
    }

    private fun initializeViews() {
        dbHelper = DatabaseHelper(this)
        rvGrades = findViewById(R.id.rvGrades)
        chart = findViewById(R.id.lineChart)
        fabAddGrade = findViewById(R.id.fabAddGrade)
        btnAddGradeType = findViewById(R.id.btnAddGradeType)
        spinnerGradeType = findViewById(R.id.spinnerGradeType)

        // Create grade input field
        gradeInputLayout = TextInputLayout(this).apply {
            hint = "Enter Grade"
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val margin = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                16f,
                resources.displayMetrics
            ).toInt()
            setPadding(margin, margin, margin, margin)
        }
        etGrade = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        gradeInputLayout.addView(etGrade)

        // Add grade input to layout after spinner
        val parentLayout = findViewById<android.view.ViewGroup>(R.id.parentLayout)
        val spinnerView = findViewById<android.view.View>(R.id.spinnerGradeType)
        val spinnerIndex = parentLayout.indexOfChild(spinnerView)
        parentLayout.addView(gradeInputLayout, spinnerIndex + 1)

        subjectId = intent.getLongExtra("subject_id", -1)
        subjectName = intent.getStringExtra("subject_name") ?: ""
        subjectTerm = intent.getStringExtra("subject_term") ?: ""

        supportActionBar?.apply {
            title = "$subjectName ($subjectTerm)"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerViews() {
        gradeAdapter = GradeRecyclerAdapter(gradeDisplayList) { gradeDisplay ->
            showEditGradeDialog(gradeDisplay)
        }
        rvGrades.layoutManager = LinearLayoutManager(this)
        rvGrades.adapter = gradeAdapter
    }

    private fun setupChart() {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.legend.isEnabled = true
        chart.axisRight.isEnabled = false
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
                saveGrade()
            }
        }

        fabAddGrade.setOnClickListener {
            if (validateInputs()) {
                saveGrade()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val gradeValidation = ValidationHelper.validateGradeString(etGrade.text.toString())
        val isTypeAvailable = !enteredTypes.contains(selectedGradeType)

        btnAddGradeType.isEnabled = gradeValidation is ValidationResult.Success && isTypeAvailable
        fabAddGrade.isEnabled = btnAddGradeType.isEnabled

        if (gradeValidation is ValidationResult.Error) {
            gradeInputLayout.error = gradeValidation.message
        } else {
            gradeInputLayout.error = null
        }

        if (!isTypeAvailable) {
            btnAddGradeType.text = "Grade for $selectedGradeType already exists"
        } else {
            btnAddGradeType.text = "Add Grade for Selected Type"
        }
        return gradeValidation is ValidationResult.Success && isTypeAvailable
    }

    private fun saveGrade() {
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
            dbHelper.insertGrade(subjectId, grade, date, type)
            enteredTypes.add(type)
            etGrade.text?.clear()
            loadData()
            DialogHelper.showSuccessDialog(this, "Grade saved successfully!")
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to save grade: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun loadData() {
        if (subjectId == -1L) {
            DialogHelper.showErrorDialog(this, "Invalid subject ID") {
                finish()
            }
            return
        }

        DialogHelper.showLoading(this, "Loading grades...")
        try {
            loadGrades()
            updateChart()
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to load grades: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun loadGrades() {
        val cursor = dbHelper.getGradesForSubject(subjectId)
        val grades = mutableListOf<Grade>()
        val gradeDisplays = mutableListOf<GradeRecyclerAdapter.GradeDisplay>()
        while (cursor.moveToNext()) {
            val grade = Grade(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                subjectId = cursor.getLong(cursor.getColumnIndexOrThrow("subject_id")),
                grade = cursor.getDouble(cursor.getColumnIndexOrThrow("grade")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            )
            grades.add(grade)
            val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            gradeDisplays.add(GradeRecyclerAdapter.GradeDisplay(grade.id, type, grade.date, grade.grade))
        }
        cursor.close()
        gradeList = grades
        gradeDisplayList = gradeDisplays
        gradeAdapter.updateGrades(gradeDisplayList)
    }

    private fun updateChart() {
        val entries = mutableListOf<Entry>()
        gradeList.forEachIndexed { index, grade ->
            entries.add(Entry(index.toFloat(), grade.grade.toFloat()))
        }

        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, "Grade Trend").apply {
                color = getColor(R.color.colorPrimary)
                setCircleColor(getColor(R.color.colorPrimary))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }

    private fun showEditGradeDialog(gradeDisplay: GradeRecyclerAdapter.GradeDisplay) {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(gradeDisplay.grade.toString())

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Grade (${gradeDisplay.type})")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val gradeStr = input.text.toString()
                val newGrade = gradeStr.toDoubleOrNull()
                if (newGrade != null && newGrade in 0.0..100.0) {
                    dbHelper.updateGrade(gradeDisplay.id, newGrade)
                    loadData()
                } else {
                    DialogHelper.showErrorDialog(this, "Enter a valid grade between 0 and 100")
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                performDeleteGrade(gradeDisplay.id)
            }
            .show()
    }

    private fun showDeleteGradeDialog(gradeDisplay: GradeRecyclerAdapter.GradeDisplay) {
        DialogHelper.showConfirmationDialog(
            context = this,
            title = "Delete Grade",
            message = "Are you sure you want to delete this grade?",
            positiveText = "Delete",
            negativeText = "Cancel"
        ) {
            performDeleteGrade(gradeDisplay.id)
        }
    }

    private fun performDeleteGrade(gradeId: Long) {
        DialogHelper.showLoading(this, "Deleting grade...")
        try {
            dbHelper.deleteGrade(gradeId)
            loadData()
            DialogHelper.showSuccessDialog(this, "Grade deleted successfully!")
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to delete grade: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
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

        fabAddGrade.setOnClickListener {
            val intent = Intent(this, AddGradeActivity::class.java).apply {
                putExtra("subject_id", subjectId)
                putExtra("subject_name", subjectName)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 