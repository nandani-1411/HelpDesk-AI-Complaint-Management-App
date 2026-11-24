package com.example.helpdeskai

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class StudentDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_student_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val email = intent.getStringExtra("STUDENT_EMAIL") ?: return
        val name = intent.getStringExtra("STUDENT_NAME") ?: "Student"

        val dbHelper = ComplaintDatabaseHelper(this)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Find the student
        val students = dbHelper.getAllStudents()
        val student = students.find { it.email.equals(email, ignoreCase = true) }
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

        // Profile card
        val tvAvatar = findViewById<TextView>(R.id.tv_detail_avatar)
        val tvName = findViewById<TextView>(R.id.tv_detail_name)
        val tvEmail = findViewById<TextView>(R.id.tv_detail_email)
        val tvStudentId = findViewById<TextView>(R.id.tv_detail_student_id)
        val tvRegDate = findViewById<TextView>(R.id.tv_detail_reg_date)
        val tvLastLogin = findViewById<TextView>(R.id.tv_detail_last_login)
        val tvStatus = findViewById<TextView>(R.id.tv_detail_status)
        val tvComplaintCount = findViewById<TextView>(R.id.tv_detail_complaint_count)

        val capName = name.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        val initials = capName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        tvAvatar.text = if (initials.isNotEmpty()) initials else "S"
        tvName.text = capName
        tvEmail.text = email

        student?.let {
            tvStudentId.text = "ID: ${if (!it.studentId.isNullOrEmpty()) it.studentId else "N/A"}"
            tvRegDate.text = "Registered: ${sdf.format(Date(it.createdAt))}"
            tvLastLogin.text = "Last Login: ${sdf.format(Date(it.lastLogin))}"
            val isActive = it.lastLogin >= sevenDaysAgo
            tvStatus.text = if (isActive) "Active" else "Inactive"
        }

        val complaints = dbHelper.getComplaintsByStudent(email)
        tvComplaintCount.text = "${complaints.size} Complaints"

        val rv = findViewById<RecyclerView>(R.id.rv_student_detail_complaints)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ComplaintAdapter(complaints) { /* read-only view */ }
    }
}
