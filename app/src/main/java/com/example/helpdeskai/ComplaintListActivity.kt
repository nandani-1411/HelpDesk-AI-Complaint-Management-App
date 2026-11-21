package com.example.helpdeskai

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class ComplaintListActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var rvComplaints: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnEmptyAction: MaterialButton
    private lateinit var fabNewComplaint: ExtendedFloatingActionButton
    private lateinit var cardStudentProfile: MaterialCardView
    private lateinit var tvStudentNameBanner: TextView
    private lateinit var tvStudentIdBanner: TextView
    private lateinit var tvStudentTicketCount: TextView
    private lateinit var tvStudentAvatar: TextView
    private lateinit var tvStudentStatPending: TextView
    private lateinit var tvStudentStatProgress: TextView
    private lateinit var tvStudentStatResolved: TextView
    private lateinit var adapter: ComplaintAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_list)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        com.example.helpdeskai.utils.NotificationHelper.createNotificationChannel(this)
    }

    override fun onResume() {
        super.onResume()
        loadComplaints()
    }

    private fun initViews() {
        rvComplaints = findViewById(R.id.rv_complaints)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        btnEmptyAction = findViewById(R.id.btn_empty_action)
        fabNewComplaint = findViewById(R.id.fab_new_complaint)
        cardStudentProfile = findViewById(R.id.card_student_profile)
        tvStudentNameBanner = findViewById(R.id.tv_student_name_banner)
        tvStudentIdBanner = findViewById(R.id.tv_student_id_banner)
        tvStudentTicketCount = findViewById(R.id.tv_student_ticket_count)
        tvStudentAvatar = findViewById(R.id.tv_student_avatar)
        tvStudentStatPending = findViewById(R.id.tv_student_stat_pending)
        tvStudentStatProgress = findViewById(R.id.tv_student_stat_progress)
        tvStudentStatResolved = findViewById(R.id.tv_student_stat_resolved)

        cardStudentProfile.setOnClickListener {
            showProfileEditDialog()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_complaints)
        toolbar.setNavigationOnClickListener {
            showStudentMenu()
        }
        toolbar.inflateMenu(R.menu.menu_student_toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_inbox) {
                startActivity(Intent(this, InboxActivity::class.java))
                true
            } else {
                false
            }
        }
    }

    private fun showStudentMenu() {
        val options = arrayOf(
            "🔔 Notification Inbox",
            "👤 My Profile & Settings",
            "➕ Raise Grievance",
            "🚪 Sign Out"
        )
        AlertDialog.Builder(this)
            .setTitle("HelpDesk Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, InboxActivity::class.java))
                    1 -> showProfileEditDialog()
                    2 -> startActivity(Intent(this, ComplaintActivity::class.java))
                    3 -> showLogoutConfirmation()
                }
            }
            .show()
    }

    private fun showNotificationCenterDialog() {
        val userEmail = sessionManager.getUser()?.email
        val list = if (!userEmail.isNullOrEmpty()) {
            dbHelper.getComplaintsByStudent(userEmail)
        } else {
            dbHelper.getAllComplaints()
        }

        if (list.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Notifications")
                .setMessage("No notifications yet. You will receive real-time alerts when officers review or resolve your complaints.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val notifications = list.map { complaint ->
            val statusIcon = when (complaint.status.lowercase()) {
                "resolved" -> "✅"
                "in progress" -> "🔄"
                else -> "⏳"
            }
            val replyInfo = if (!complaint.officerReply.isNullOrBlank()) {
                "\n💬 Officer: ${complaint.officerReply}"
            } else ""

            "$statusIcon #${complaint.id}: ${complaint.title}\nStatus: ${complaint.status.uppercase()}$replyInfo"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🔔 Grievance Activity & Alerts")
            .setItems(notifications) { _, which ->
                // Tapping item can highlight or show detail
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showProfileEditDialog() {
        val user = sessionManager.getUser() ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_profile, null)
        val tvRole = dialogView.findViewById<TextView>(R.id.tv_dialog_profile_role)
        val etName = dialogView.findViewById<EditText>(R.id.et_profile_name)
        val etId = dialogView.findViewById<EditText>(R.id.et_profile_id)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_profile_email)
        val tvIdLabel = dialogView.findViewById<TextView>(R.id.tv_profile_id_label)

        tvRole.text = "Student Account"
        tvIdLabel.text = "Student Roll / Enrollment No."
        etName.setText(user.name)
        etId.setText(user.studentId ?: "")
        etEmail.setText(user.email)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save Changes") { _, _ ->
                val newName = etName.text.toString().trim()
                val newId = etId.text.toString().trim()
                if (newName.isNotEmpty()) {
                    dbHelper.updateUserProfile(user.email, newName, newId)
                    sessionManager.updateUserProfile(newName, newId)
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    loadComplaints()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Do you want to log out of your student account?")
            .setPositiveButton("Logout") { _, _ ->
                sessionManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        rvComplaints.layoutManager = LinearLayoutManager(this)
        adapter = ComplaintAdapter(emptyList()) { complaint ->
            // Card clicked - can view details
        }
        rvComplaints.adapter = adapter
    }

    private fun setupListeners() {
        fabNewComplaint.setOnClickListener {
            startActivity(Intent(this, ComplaintActivity::class.java))
        }

        btnEmptyAction.setOnClickListener {
            startActivity(Intent(this, ComplaintActivity::class.java))
        }
    }

    private fun loadComplaints() {
        val user = sessionManager.getUser()
        if (user != null) {
            tvStudentNameBanner.text = user.name
            val idDisplay = if (!user.studentId.isNullOrEmpty()) user.studentId else "Student"
            tvStudentIdBanner.text = "ID: $idDisplay • Tap to edit profile"
            findViewById<Toolbar>(R.id.toolbar_complaints).subtitle = "${user.name} ($idDisplay)"
            // Set initials avatar
            val initials = user.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            tvStudentAvatar.text = if (initials.isNotEmpty()) initials else "S"
        }

        val userEmail = user?.email
        val list = if (!userEmail.isNullOrEmpty()) {
            dbHelper.getComplaintsByStudent(userEmail)
        } else {
            dbHelper.getAllComplaints()
        }

        tvStudentTicketCount.text = "${list.size} ${if (list.size == 1) "Ticket" else "Tickets"}"

        // Populate stats
        val pending = list.count { it.status.equals("Pending", ignoreCase = true) }
        val inProgress = list.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolved = list.count { it.status.equals("Resolved", ignoreCase = true) }
        tvStudentStatPending.text = pending.toString()
        tvStudentStatProgress.text = inProgress.toString()
        tvStudentStatResolved.text = resolved.toString()

        if (list.isEmpty()) {
            rvComplaints.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvComplaints.visibility = View.VISIBLE
            adapter.updateList(list)
        }
    }
}
