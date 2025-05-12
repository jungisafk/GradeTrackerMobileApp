package com.example.gradetracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalRecyclerAdapter(
    private var goals: List<GoalDisplay>,
    private val onEditClick: (GoalDisplay) -> Unit,
    private val onDeleteClick: (GoalDisplay) -> Unit
) : RecyclerView.Adapter<GoalRecyclerAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalSubject: TextView = itemView.findViewById(R.id.tvGoalSubject)
        val tvGoalCurrent: TextView = itemView.findViewById(R.id.tvGoalCurrent)
        val tvGoalTarget: TextView = itemView.findViewById(R.id.tvGoalTarget)
        val tvGoalStatus: TextView = itemView.findViewById(R.id.tvGoalStatus)
        val progressGoal: ProgressBar = itemView.findViewById(R.id.progressGoal)
        val btnEditGoal: ImageButton = itemView.findViewById(R.id.btnEditGoal)
        val btnDeleteGoal: ImageButton = itemView.findViewById(R.id.btnDeleteGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvGoalSubject.text = goal.subjectName
        holder.tvGoalCurrent.text = "Current: %.2f".format(goal.current)
        holder.tvGoalTarget.text = "Target: %.2f".format(goal.target)
        holder.progressGoal.progress = ((goal.current / goal.target) * 100).toInt().coerceAtMost(100)
        if (goal.achieved) {
            holder.tvGoalStatus.text = "Achieved"
            holder.tvGoalStatus.setTextColor(Color.parseColor("#388E3C")) // Green
        } else if (goal.current >= goal.target * 0.8) {
            holder.tvGoalStatus.text = "Close"
            holder.tvGoalStatus.setTextColor(Color.parseColor("#FFA000")) // Amber
        } else {
            holder.tvGoalStatus.text = "Below"
            holder.tvGoalStatus.setTextColor(Color.parseColor("#D32F2F")) // Red
        }
        holder.btnEditGoal.setOnClickListener { onEditClick(goal) }
        holder.btnDeleteGoal.setOnClickListener { onDeleteClick(goal) }
    }

    override fun getItemCount(): Int = goals.size

    fun updateGoals(newGoals: List<GoalDisplay>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}
