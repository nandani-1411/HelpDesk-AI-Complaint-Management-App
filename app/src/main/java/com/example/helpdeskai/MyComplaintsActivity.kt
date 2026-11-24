package com.example.helpdeskai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.chip.ChipGroup

class MyComplaintsActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var rvComplaints: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: ComplaintAdapter
    private var allComplaints: List<Complaint> = emptyList()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_complaints)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_my_complaints)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvComplaints = findViewById(R.id.rv_my_complaints_list)
        layoutEmpty = findViewById(R.id.layout_my_complaints_empty)
        chipGroup = findViewById(R.id.chip_group_my_complaints)

        rvComplaints.layoutManager = LinearLayoutManager(this)
        adapter = ComplaintAdapter(emptyList()) { complaint ->
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("COMPLAINT_ID", complaint.id)
            startActivity(intent)
        }
        rvComplaints.adapter = adapter

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentFilter = when (checkedIds[0]) {
                R.id.chip_mc_pending -> "Pending"
                R.id.chip_mc_progress -> "In Progress"
                R.id.chip_mc_resolved -> "Resolved"
                else -> "All"
            }
            applyFilter()
        }

        loadComplaints()
    }

    override fun onResume() {
        super.onResume()
        loadComplaints()
    }

    private fun loadComplaints() {
        val email = sessionManager.getUserEmail()
        allComplaints = if (email.isNotEmpty()) {
            dbHelper.getComplaintsByStudent(email)
        } else {
            emptyList()
        }
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "Pending" -> allComplaints.filter { it.status.equals("Pending", ignoreCase = true) }
            "In Progress" -> allComplaints.filter { it.status.equals("In Progress", ignoreCase = true) }
            "Resolved" -> allComplaints.filter { it.status.equals("Resolved", ignoreCase = true) }
            else -> allComplaints
        }

        if (filtered.isEmpty()) {
            rvComplaints.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            rvComplaints.visibility = View.VISIBLE
            adapter.updateList(filtered)
        }
    }
}
