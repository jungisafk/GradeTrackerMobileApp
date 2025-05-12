package com.example.gradetracker

import android.content.Intent
import android.os.Bundle
import android.view.Menu
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
import com.example.gradetracker.models.Subject
import com.example.gradetracker.utils.DialogHelper
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var lvSubjects: RecyclerView
    private lateinit var chart: LineChart
    private lateinit var fabAddGrade: FloatingActionButton
    private lateinit var subjectAdapter: SubjectRecyclerAdapter
    private var subjectList: List<Subject> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupRecyclerViews()
        setupChart()
        setupNavigation()
        loadData()
    }

    private fun initializeViews() {
        dbHelper = DatabaseHelper(this)
        lvSubjects = findViewById(R.id.lvSubjects)
        chart = findViewById(R.id.chart)
        fabAddGrade = findViewById(R.id.fabAddGrade)
    }

    private fun setupRecyclerViews() {
        subjectAdapter = SubjectRecyclerAdapter(subjectList) { subject ->
            showSubjectDetails(subject)
        }
        lvSubjects.layoutManager = LinearLayoutManager(this)
        lvSubjects.adapter = subjectAdapter
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

    private fun loadData() {
        DialogHelper.showLoading(this, "Loading data...")
        try {
            loadSubjects()
            updateChart()
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(this, "Failed to load data: ${e.message}")
        } finally {
            DialogHelper.hideLoading()
        }
    }

    private fun loadSubjects() {
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
    }

    private fun updateChart() {
        val entries = mutableListOf<Entry>()
        val cursor = dbHelper.getAllGrades()
        var index = 0f
        while (cursor.moveToNext()) {
            val grade = cursor.getDouble(cursor.getColumnIndexOrThrow("grade"))
            entries.add(Entry(index, grade.toFloat()))
            index++
        }
        cursor.close()

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

    private fun showSubjectDetails(subject: Subject) {
        val intent = Intent(this, SubjectDetailActivity::class.java).apply {
            putExtra("subject_id", subject.id)
            putExtra("subject_name", subject.name)
            putExtra("subject_term", subject.term)
        }
        startActivity(intent)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
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
            startActivity(Intent(this, AddGradeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}