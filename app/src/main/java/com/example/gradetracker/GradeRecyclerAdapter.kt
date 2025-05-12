package com.example.gradetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GradeRecyclerAdapter(
    private var grades: List<GradeDisplay>,
    private val onEditClick: (GradeDisplay) -> Unit
) : RecyclerView.Adapter<GradeRecyclerAdapter.GradeViewHolder>() {

    data class GradeDisplay(val id: Long, val date: String, val type: String, val grade: Double)

    inner class GradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGradeDate: TextView = itemView.findViewById(R.id.tvGradeDate)
        val tvGradeValue: TextView = itemView.findViewById(R.id.tvGradeValue)
        val tvGradeType: TextView = itemView.findViewById(R.id.tvGradeType)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditGrade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grade, parent, false)
        return GradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: GradeViewHolder, position: Int) {
        val grade = grades[position]
        holder.tvGradeDate.text = grade.type
        holder.tvGradeValue.text = grade.grade.toString()
        holder.tvGradeType.text = grade.date
        holder.btnEdit.setOnClickListener { onEditClick(grade) }
    }

    override fun getItemCount(): Int = grades.size

    fun updateGrades(newGrades: List<GradeDisplay>) {
        grades = newGrades
        notifyDataSetChanged()
    }
} 