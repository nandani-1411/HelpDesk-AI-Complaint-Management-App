package com.example.helpdeskai

import android.app.AlertDialog
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatPending: TextView
    private lateinit var tvStatResolved: TextView
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var rvAdminComplaints: RecyclerView
    private lateinit var layoutAdminEmptyState: LinearLayout
    private lateinit var adapter: ComplaintAdapter

    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        dbHelper = ComplaintDatabaseHelper(this)

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
        tvStatResolved = findViewById(R.id.tv_stat_resolved)
        chipGroupFilters = findViewById(R.id.chip_group_filters)
        rvAdminComplaints = findViewById(R.id.rv_admin_complaints)
        layoutAdminEmptyState = findViewById(R.id.layout_admin_empty_state)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_admin)
        toolbar.setNavigationOnClickListener {
            finish()
        }
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
        val allComplaints = dbHelper.getAllComplaints()
        val pendingCount = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val resolvedCount = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        tvStatTotal.text = allComplaints.size.toString()
        tvStatPending.text = pendingCount.toString()
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
        val rgStatus = dialogView.findViewById<RadioGroup>(R.id.rg_status)
        val etReply = dialogView.findViewById<EditText>(R.id.et_officer_reply)
        val btnAiDraft = dialogView.findViewById<MaterialButton>(R.id.btn_ai_draft_reply)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pb_admin_ai_loading)

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
            etReply.hint = "⏳ Groq AI is drafting official reply..."

            GroqAiService.generateAdminReply(
                complaintTitle       = complaint.title,
                complaintDescription = complaint.description,
                complaintCategory    = complaint.category,
                onSuccess = { generatedReply ->
                    pbLoading.visibility = View.GONE
                    btnAiDraft.isEnabled = true
                    etReply.hint = "Officer reply"
                    etReply.setText(generatedReply)
                    // Auto-select In Progress when official response is drafted
                    dialogView.findViewById<RadioButton>(R.id.rb_in_progress).isChecked = true
                    Toast.makeText(this, "✨ Groq AI drafted official resolution!", Toast.LENGTH_SHORT).show()
                },
                onError = { errorMessage ->
                    pbLoading.visibility = View.GONE
                    btnAiDraft.isEnabled = true
                    etReply.hint = "Officer reply"
                    Toast.makeText(this, "Groq AI: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Manage Complaint #${complaint.id}")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val selectedStatus = when (rgStatus.checkedRadioButtonId) {
                    R.id.rb_resolved -> "Resolved"
                    R.id.rb_in_progress -> "In Progress"
                    else -> "Pending"
                }
                val replyText = etReply.text.toString().trim()
                dbHelper.updateStatusAndReply(
                    complaint.id,
                    selectedStatus,
                    if (replyText.isNotEmpty()) replyText else null
                )
                Toast.makeText(this, "Status updated to $selectedStatus", Toast.LENGTH_SHORT).show()
                refreshDashboard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
