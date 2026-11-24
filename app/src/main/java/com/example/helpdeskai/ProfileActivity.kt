package com.example.helpdeskai

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val user = sessionManager.getUser() ?: return

        val tvAvatar = findViewById<TextView>(R.id.tv_page_profile_avatar)
        val tvName = findViewById<TextView>(R.id.tv_page_profile_name)
        val tvRole = findViewById<TextView>(R.id.tv_page_profile_role)
        val etName = findViewById<EditText>(R.id.et_page_profile_name)
        val tvIdLabel = findViewById<TextView>(R.id.tv_page_profile_id_label)
        val etId = findViewById<EditText>(R.id.et_page_profile_id)
        val etEmail = findViewById<EditText>(R.id.et_page_profile_email)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_profile_page)

        val rawName = if (user.role.equals(User.ROLE_ADMIN, ignoreCase = true) && (user.name.contains("Grievance", ignoreCase = true) || user.name.contains("Officer", ignoreCase = true))) {
            "Support Team"
        } else {
            user.name
        }
        val capName = rawName.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        val initials = capName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        tvAvatar.text = if (initials.isNotEmpty()) initials else "U"
        tvName.text = capName
        etName.setText(capName)
        etEmail.setText(user.email)

        if (user.role.equals(User.ROLE_ADMIN, ignoreCase = true)) {
            tvRole.text = "SUPPORT TEAM"
            tvIdLabel.text = "Staff ID"
            etId.setText(user.studentId ?: "STAFF-001")
        } else {
            tvRole.text = "STUDENT"
            tvIdLabel.text = "Student Roll / Enrollment No."
            etId.setText(user.studentId ?: "")
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newId = etId.text.toString().trim()
            if (newName.isNotEmpty()) {
                dbHelper.updateUserProfile(user.email, newName, newId)
                sessionManager.updateUserProfile(newName, newId)
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                tvName.text = newName
            }
        }
    }
}
