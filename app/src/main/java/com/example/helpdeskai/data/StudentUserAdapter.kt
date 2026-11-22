package com.example.helpdeskai.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.R
import com.example.helpdeskai.data.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentUserAdapter(
    private var users: List<User>
) : RecyclerView.Adapter<StudentUserAdapter.StudentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatar: TextView = itemView.findViewById(R.id.tv_user_item_avatar)
        val tvName: TextView = itemView.findViewById(R.id.tv_user_item_name)
        val tvEmail: TextView = itemView.findViewById(R.id.tv_user_item_email)
        val tvMeta: TextView = itemView.findViewById(R.id.tv_user_item_meta)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_user_item_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_user, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.name
        holder.tvEmail.text = user.email

        val initials = user.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        holder.tvAvatar.text = if (initials.isNotEmpty()) initials else "S"

        val regDate = dateFormat.format(Date(user.createdAt))
        val lastDate = if (user.lastLogin > 0) dateFormat.format(Date(user.lastLogin)) else "Recently"
        val idStr = if (!user.studentId.isNullOrEmpty()) user.studentId else "N/A"
        holder.tvMeta.text = "ID: $idStr\nRegistered: $regDate\nLast Active: $lastDate"

        // Active if last login was within last 7 days
        val isRecent = user.lastLogin >= System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        if (isRecent) {
            holder.tvStatus.text = "ACTIVE"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_resolved)
        } else {
            holder.tvStatus.text = "OFFLINE"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }
}
