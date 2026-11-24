package com.example.helpdeskai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.StudentDirectoryAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.chip.ChipGroup

class StudentsActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var rvStudents: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var tvCount: TextView

    private var allStudents: List<User> = emptyList()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        if (!sessionManager.getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)) {
            finish()
            return
        }

        setContentView(R.layout.activity_students)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_students)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvStudents = findViewById(R.id.rv_students)
        layoutEmpty = findViewById(R.id.layout_students_empty)
        etSearch = findViewById(R.id.et_students_search)
        chipGroup = findViewById(R.id.chip_group_students)
        tvCount = findViewById(R.id.tv_students_count)

        rvStudents.layoutManager = LinearLayoutManager(this)

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentFilter = when (checkedIds[0]) {
                R.id.chip_students_active -> "Active"
                R.id.chip_students_inactive -> "Inactive"
                else -> "All"
            }
            applyFilter()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyFilter() }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadStudents()
    }

    override fun onResume() {
        super.onResume()
        loadStudents()
    }

    private fun loadStudents() {
        allStudents = dbHelper.getAllStudents()
        applyFilter()
    }

    private fun applyFilter() {
        val query = etSearch.text.toString().trim().lowercase()
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

        var filtered = allStudents
        if (currentFilter == "Active") {
            filtered = filtered.filter { it.lastLogin >= sevenDaysAgo }
        } else if (currentFilter == "Inactive") {
            filtered = filtered.filter { it.lastLogin < sevenDaysAgo }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.email.lowercase().contains(query) ||
                (it.studentId?.lowercase()?.contains(query) == true)
            }
        }

        tvCount.text = "${filtered.size} Students"

        val adapter = StudentDirectoryAdapter(filtered) { student ->
            val intent = Intent(this, StudentDetailActivity::class.java)
            intent.putExtra("STUDENT_EMAIL", student.email)
            intent.putExtra("STUDENT_NAME", student.name)
            startActivity(intent)
        }
        rvStudents.adapter = adapter

        if (filtered.isEmpty()) {
            rvStudents.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            rvStudents.visibility = View.VISIBLE
        }
    }
}
