package com.example.helpdeskai

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.GroqAiService
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatPending: TextView
    private lateinit var tvStatProgress: TextView
    private lateinit var tvStatResolved: TextView
    private lateinit var tvAdminWelcomeName: TextView
    private lateinit var tvAdminEmailSub: TextView
    private lateinit var cardAdminProfile: MaterialCardView
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var rvAdminComplaints: RecyclerView
    private lateinit var layoutAdminEmptyState: LinearLayout
    private lateinit var adapter: ComplaintAdapter

    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun initViews() {
        tvStatTotal = findViewById(R.id.tv_stat_total)
        tvStatPending = findViewById(R.id.tv_stat_pending)
        tvStatProgress = findViewById(R.id.tv_stat_progress)
        tvStatResolved = findViewById(R.id.tv_stat_resolved)
        tvAdminWelcomeName = findViewById(R.id.tv_admin_welcome_name)
        tvAdminEmailSub = findViewById(R.id.tv_admin_email_sub)
        cardAdminProfile = findViewById(R.id.card_admin_profile)
        chipGroupFilters = findViewById(R.id.chip_group_filters)
        rvAdminComplaints = findViewById(R.id.rv_admin_complaints)
        layoutAdminEmptyState = findViewById(R.id.layout_admin_empty_state)

        cardAdminProfile.setOnClickListener {
            showProfileEditDialog()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_admin)
        toolbar.setNavigationOnClickListener {
            showAdminMenu()
        }
    }

    private fun showAdminMenu() {
        val options = arrayOf("👤 My Profile & Settings", "🚪 Sign Out")
        AlertDialog.Builder(this)
            .setTitle("Admin Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showProfileEditDialog()
                    1 -> showLogoutConfirmation()
                }
            }
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

        tvRole.text = "Grievance Officer (Administrator)"
        tvIdLabel.text = "Staff / Officer ID"
        etName.setText(user.name)
        etId.setText(user.studentId ?: "STAFF-001")
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
                    refreshDashboard()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to log out of the Admin Portal?")
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
        rvAdminComplaints.layoutManager = LinearLayoutManager(this)
        adapter = ComplaintAdapter(emptyList()) { complaint ->
            showStatusUpdateDialog(complaint)
        }
        rvAdminComplaints.adapter = adapter
    }

    private fun setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentFilter = when (checkedIds[0]) {
                R.id.chip_filter_pending -> "Pending"
                R.id.chip_filter_in_progress -> "In Progress"
                R.id.chip_filter_resolved -> "Resolved"
                else -> "All"
            }
            filterComplaints()
        }
    }

    private fun refreshDashboard() {
        val user = sessionManager.getUser()
        tvAdminWelcomeName.text = user?.name ?: "Administrator"
        tvAdminEmailSub.text = "${user?.email ?: "admin@gmail.com"} • Tap to Edit"

        val allComplaints = dbHelper.getAllComplaints()
        val pendingCount = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val progressCount = allComplaints.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolvedCount = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        tvStatTotal.text = allComplaints.size.toString()
        tvStatPending.text = pendingCount.toString()
        tvStatProgress.text = progressCount.toString()
        tvStatResolved.text = resolvedCount.toString()

        filterComplaints()
    }

    private fun filterComplaints() {
        val list = if (currentFilter == "All") {
            dbHelper.getAllComplaints()
        } else {
            dbHelper.getComplaintsByStatus(currentFilter)
        }

        if (list.isEmpty()) {
            rvAdminComplaints.visibility = View.GONE
            layoutAdminEmptyState.visibility = View.VISIBLE
        } else {
            layoutAdminEmptyState.visibility = View.GONE
            rvAdminComplaints.visibility = View.VISIBLE
            adapter.updateList(list)
        }
    }

    private fun showStatusUpdateDialog(complaint: Complaint) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_officer_reply, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_dialog_subtitle)
        val rgStatus = dialogView.findViewById<RadioGroup>(R.id.rg_status)
        val etReply = dialogView.findViewById<EditText>(R.id.et_officer_reply)
        val btnAiDraft = dialogView.findViewById<MaterialButton>(R.id.btn_ai_draft_reply)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pb_admin_ai_loading)

        // Populate Complaint Details
        tvTitle.text = "Grievance #${complaint.id}: ${complaint.title}"
        tvSubtitle.text = "Category: ${complaint.category} • Student: ${complaint.studentEmail ?: "Registered Student"}"

        // Set current status
        when (complaint.status.lowercase()) {
            "resolved" -> dialogView.findViewById<RadioButton>(R.id.rb_resolved).isChecked = true
            "in progress" -> dialogView.findViewById<RadioButton>(R.id.rb_in_progress).isChecked = true
            else -> dialogView.findViewById<RadioButton>(R.id.rb_pending).isChecked = true
        }

        if (!complaint.officerReply.isNullOrEmpty()) {
            etReply.setText(complaint.officerReply)
        }

        // Groq AI Auto-Draft Administrative Response
        btnAiDraft.setOnClickListener {
            pbLoading.visibility = View.VISIBLE
            btnAiDraft.isEnabled = false
            etReply.setText("")
            etReply.hint = "⏳ Groq AI is drafting official resolution..."

            GroqAiService.generateAdminReply(
                complaintTitle = complaint.title,
                complaintDescription = complaint.description,
                complaintCategory = complaint.category,
                onSuccess = { generatedReply ->
                    pbLoading.visibility = View.GONE
                    btnAiDraft.isEnabled = true
                    etReply.hint = "Type official resolution..."
                    etReply.setText(generatedReply)
                    // Auto-select In Progress when official response is drafted
                    dialogView.findViewById<RadioButton>(R.id.rb_in_progress).isChecked = true
                    Toast.makeText(this, "✨ Groq AI drafted official resolution!", Toast.LENGTH_SHORT).show()
                },
                onError = { errorMessage ->
                    pbLoading.visibility = View.GONE
                    btnAiDraft.isEnabled = true
                    etReply.hint = "Type official resolution..."
                    Toast.makeText(this, "Groq AI: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save Status") { _, _ ->
                val selectedStatus = when (rgStatus.checkedRadioButtonId) {
                    R.id.rb_resolved -> "Resolved"
                    R.id.rb_in_progress -> "In Progress"
                    else -> "Pending"
                }
                val replyText = etReply.text.toString().trim()
                val finalReply = if (replyText.isNotEmpty()) replyText else null
                dbHelper.updateStatusAndReply(
                    complaint.id,
                    selectedStatus,
                    finalReply
                )
                com.example.helpdeskai.utils.NotificationHelper.sendStatusUpdateNotification(
                    context = this,
                    complaintId = complaint.id,
                    title = complaint.title,
                    newStatus = selectedStatus,
                    officerReply = finalReply
                )
                Toast.makeText(this, "Ticket #${complaint.id} updated to $selectedStatus", Toast.LENGTH_SHORT).show()
                refreshDashboard()
            }
            .setNeutralButton("Delete") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Ticket #${complaint.id}?")
                    .setMessage("Are you sure you want to permanently delete this complaint ticket?")
                    .setPositiveButton("Yes, Delete") { _, _ ->
                        dbHelper.deleteComplaint(complaint.id)
                        Toast.makeText(this, "Complaint #${complaint.id} deleted", Toast.LENGTH_SHORT).show()
                        refreshDashboard()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
