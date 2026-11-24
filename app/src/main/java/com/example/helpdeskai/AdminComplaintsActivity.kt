package com.example.helpdeskai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.chip.ChipGroup

class AdminComplaintsActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var rvComplaints: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: ComplaintAdapter
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        if (!sessionManager.getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)) {
            finish()
            return
        }
        dbHelper = ComplaintDatabaseHelper(this)
        setContentView(R.layout.activity_admin_complaints)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_admin_complaints)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvComplaints = findViewById(R.id.rv_admin_complaints_page)
        layoutEmpty = findViewById(R.id.layout_admin_complaints_empty)
        etSearch = findViewById(R.id.et_admin_complaints_search)
        chipGroup = findViewById(R.id.chip_group_admin_complaints)

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
                R.id.chip_ac_pending -> "Pending"
                R.id.chip_ac_progress -> "In Progress"
                R.id.chip_ac_resolved -> "Resolved"
                else -> "All"
            }
            loadComplaints()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { loadComplaints() }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadComplaints()
    }

    override fun onResume() {
        super.onResume()
        loadComplaints()
    }

    private fun loadComplaints() {
        val query = etSearch.text.toString().trim()
        val list = if (query.isNotEmpty()) {
            dbHelper.searchComplaints(query, currentFilter)
        } else {
            if (currentFilter == "All") dbHelper.getAllComplaints()
            else dbHelper.getComplaintsByStatus(currentFilter)
        }

        if (list.isEmpty()) {
            rvComplaints.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            rvComplaints.visibility = View.VISIBLE
            adapter.updateList(list)
        }
    }
}
