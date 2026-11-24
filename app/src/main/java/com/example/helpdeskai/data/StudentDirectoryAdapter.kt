package com.example.helpdeskai.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.R
import com.example.helpdeskai.data.model.User
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class StudentDirectoryAdapter(
    private val students: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<StudentDirectoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tv_dir_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_dir_name)
        val tvEmail: TextView = view.findViewById(R.id.tv_dir_email)
        val tvStudentId: TextView = view.findViewById(R.id.tv_dir_student_id)
        val tvRegDate: TextView = view.findViewById(R.id.tv_dir_reg_date)
        val tvLastLogin: TextView = view.findViewById(R.id.tv_dir_last_login)
        val tvStatus: TextView = view.findViewById(R.id.tv_dir_status)
        val card: MaterialCardView = view.findViewById(R.id.card_student_dir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_directory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val isActive = student.lastLogin >= sevenDaysAgo

        val initials = student.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        holder.tvAvatar.text = if (initials.isNotEmpty()) initials else "S"
        holder.tvName.text = student.name
        holder.tvEmail.text = student.email
        holder.tvStudentId.text = if (!student.studentId.isNullOrEmpty()) student.studentId else "N/A"
        holder.tvRegDate.text = "Joined: ${sdf.format(Date(student.createdAt))}"
        holder.tvLastLogin.text = "Last seen: ${sdf.format(Date(student.lastLogin))}"

        val ctx = holder.itemView.context
        if (isActive) {
            holder.tvStatus.text = "Active"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_resolved)
            holder.tvStatus.setTextColor(ctx.getColor(R.color.status_resolved))
        } else {
            holder.tvStatus.text = "Inactive"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            holder.tvStatus.setTextColor(ctx.getColor(R.color.status_pending))
        }

        holder.card.setOnClickListener { onClick(student) }
    }

    override fun getItemCount() = students.size
}
