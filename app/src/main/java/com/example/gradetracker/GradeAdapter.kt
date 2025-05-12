package com.example.gradetracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class GradeAdapter(context: Context, grades: List<String>) : ArrayAdapter<String>(context, 0, grades) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val gradeString = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_grade, parent, false)
        val tvGradeDate = view.findViewById<TextView>(R.id.tvGradeDate)
        val tvGradeValue = view.findViewById<TextView>(R.id.tvGradeValue)
        val parts = gradeString?.split(": ")
        if (parts?.size == 2) {
            tvGradeDate.text = parts[0]
            tvGradeValue.text = parts[1]
        }
        return view
    }
} 