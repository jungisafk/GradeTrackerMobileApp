package com.example.gradetracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gradetracker.models.Subject

class SubjectRecyclerAdapter(
    private var subjects: List<Subject>,
    private val onItemClick: (Subject) -> Unit
) : RecyclerView.Adapter<SubjectRecyclerAdapter.SubjectViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubjectName: TextView = itemView.findViewById(R.id.tvSubjectName)
        val tvSubjectTerm: TextView = itemView.findViewById(R.id.tvSubjectTerm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.tvSubjectName.text = subject.name
        holder.tvSubjectTerm.text = subject.term
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(subject)
        }
        // Highlight selected item
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int = subjects.size

    fun updateSubjects(newSubjects: List<Subject>) {
        subjects = newSubjects
        notifyDataSetChanged()
    }
} 