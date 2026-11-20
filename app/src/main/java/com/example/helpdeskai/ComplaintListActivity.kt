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
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class ComplaintListActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var rvComplaints: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnEmptyAction: MaterialButton
    private lateinit var fabNewComplaint: ExtendedFloatingActionButton
    private lateinit var adapter: ComplaintAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_list)

        dbHelper = ComplaintDatabaseHelper(this)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
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
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_complaints)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        rvComplaints.layoutManager = LinearLayoutManager(this)
        adapter = ComplaintAdapter(emptyList()) { complaint ->
            // Card click - could show details or toast
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
        val list = dbHelper.getAllComplaints()
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
