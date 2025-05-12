package com.example.gradetracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gradetracker.models.Subject

class SubjectAdapter(context: Context, subjects: List<Subject>) : ArrayAdapter<Subject>(context, 0, subjects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val subject = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_subject, parent, false)
        val tvSubjectName = view.findViewById<TextView>(R.id.tvSubjectName)
        val tvSubjectTerm = view.findViewById<TextView>(R.id.tvSubjectTerm)
        tvSubjectName.text = subject?.name
        tvSubjectTerm.text = subject?.term
        return view
    }
} 