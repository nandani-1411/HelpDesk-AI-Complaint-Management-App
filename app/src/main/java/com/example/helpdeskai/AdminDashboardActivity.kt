package com.example.helpdeskai

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.StudentUserAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.GroqAiService
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatPending: TextView
    private lateinit var tvStatProgress: TextView
    private lateinit var tvStatResolved: TextView
    private lateinit var tvAdminWelcomeName: TextView
    private lateinit var tvAdminEmailSub: TextView
    private lateinit var cardAdminProfile: MaterialCardView

    // Student Intelligence views
    private lateinit var tvAdminStatStudents: TextView
    private lateinit var tvAdminStatActive: TextView
    private lateinit var tvAdminStatNewReg: TextView
    private lateinit var btnManageStudents: MaterialButton

    // Analytics breakdown
    private lateinit var layoutAnalyticsBars: LinearLayout
    private lateinit var tvAnalyticsSummary: TextView

    // Search & Filter
    private lateinit var etAdminSearch: EditText
    private lateinit var ivAdminClearSearch: ImageView
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var rvAdminComplaints: RecyclerView
    private lateinit var layoutAdminEmptyState: LinearLayout
    private lateinit var adapter: ComplaintAdapter

    // Drawer Views
    private lateinit var tvAdminDrawerName: TextView
    private lateinit var tvAdminDrawerEmail: TextView

    private var currentFilter = "All"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        // Strict role-based route protection
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        if (!sessionManager.getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)) {
            Toast.makeText(this, "Access Denied: Administrator role required.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ComplaintListActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_admin_dashboard)

        initViews()
        setupToolbar()
        setupDrawerNavigation()
        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupBackPressHandler()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout_admin)
        tvStatTotal = findViewById(R.id.tv_stat_total)
        tvStatPending = findViewById(R.id.tv_stat_pending)
        tvStatProgress = findViewById(R.id.tv_stat_progress)
        tvStatResolved = findViewById(R.id.tv_stat_resolved)
        tvAdminWelcomeName = findViewById(R.id.tv_admin_welcome_name)
        tvAdminEmailSub = findViewById(R.id.tv_admin_email_sub)
        cardAdminProfile = findViewById(R.id.card_admin_profile)

        tvAdminStatStudents = findViewById(R.id.tv_admin_stat_students)
        tvAdminStatActive = findViewById(R.id.tv_admin_stat_active)
        tvAdminStatNewReg = findViewById(R.id.tv_admin_stat_new_reg)
        btnManageStudents = findViewById(R.id.btn_manage_students)

        layoutAnalyticsBars = findViewById(R.id.layout_analytics_bars)
        tvAnalyticsSummary = findViewById(R.id.tv_analytics_summary)

        etAdminSearch = findViewById(R.id.et_admin_search)
        ivAdminClearSearch = findViewById(R.id.iv_admin_clear_search)
        chipGroupFilters = findViewById(R.id.chip_group_filters)
        rvAdminComplaints = findViewById(R.id.rv_admin_complaints)
        layoutAdminEmptyState = findViewById(R.id.layout_admin_empty_state)

        tvAdminDrawerName = findViewById(R.id.tv_admin_drawer_name)
        tvAdminDrawerEmail = findViewById(R.id.tv_admin_drawer_email)

        cardAdminProfile.setOnClickListener {
            showProfileEditDialog()
        }

        btnManageStudents.setOnClickListener {
            showStudentManagementDialog()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_admin)
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun setupDrawerNavigation() {
        findViewById<View>(R.id.nav_admin_item_cmd)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.nav_admin_item_students)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showStudentManagementDialog()
        }
        findViewById<View>(R.id.nav_admin_item_analytics)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            findViewById<View>(R.id.card_category_analytics)?.requestFocus()
        }
        findViewById<View>(R.id.nav_admin_item_settings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showProfileEditDialog()
        }
        findViewById<View>(R.id.nav_admin_item_logout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showLogoutConfirmation()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupSearch() {
        etAdminSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                ivAdminClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterComplaints()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivAdminClearSearch.setOnClickListener {
            etAdminSearch.setText("")
        }
    }

    private fun showStudentManagementDialog() {
        val students = dbHelper.getAllStudents()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_management, null)
        val tvBanner = dialogView.findViewById<TextView>(R.id.tv_dialog_students_count_banner)
        val rvStudents = dialogView.findViewById<RecyclerView>(R.id.rv_dialog_students)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tv_dialog_students_empty)

        val total = dbHelper.getTotalStudentsCount()
        val active = dbHelper.getActiveStudentsCount()
        tvBanner.text = "Registered Students: $total • Active This Week: $active"

        rvStudents.layoutManager = LinearLayoutManager(this)
        val studentAdapter = StudentUserAdapter(students)
        rvStudents.adapter = studentAdapter

        if (students.isEmpty()) {
            rvStudents.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            rvStudents.visibility = View.VISIBLE
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
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
            .setMessage("Are you sure you want to log out of the Admin Command Center?")
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
        tvAdminWelcomeName.text = user?.name ?: "Grievance Officer"
        tvAdminEmailSub.text = "${user?.email ?: "admin@gmail.com"} • Tap to Edit Profile"

        // Drawer Bindings
        tvAdminDrawerName.text = user?.name ?: "Grievance Officer"
        tvAdminDrawerEmail.text = user?.email ?: "admin@gmail.com"

        // Real Database Student Metrics
        val totalStudents = dbHelper.getTotalStudentsCount()
        val activeStudents = dbHelper.getActiveStudentsCount()
        val newRegistrations = dbHelper.getNewRegistrationsCount()

        tvAdminStatStudents.text = totalStudents.toString()
        tvAdminStatActive.text = activeStudents.toString()
        tvAdminStatNewReg.text = newRegistrations.toString()

        // Real Database Complaint Metrics
        val allComplaints = dbHelper.getAllComplaints()
        val pendingCount = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val progressCount = allComplaints.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolvedCount = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        tvStatTotal.text = allComplaints.size.toString()
        tvStatPending.text = pendingCount.toString()
        tvStatProgress.text = progressCount.toString()
        tvStatResolved.text = resolvedCount.toString()

        renderCategoryAnalytics(allComplaints)
        filterComplaints()
    }

    private fun renderCategoryAnalytics(allComplaints: List<Complaint>) {
        layoutAnalyticsBars.removeAllViews()
        val total = allComplaints.size
        if (total == 0) {
            tvAnalyticsSummary.text = "No grievances filed yet across campus departments"
            return
        }

        val dist = dbHelper.getCategoryDistribution()
        tvAnalyticsSummary.text = "${dist.size} active campus departments • Total $total tickets"

        val colors = listOf(
            R.color.primary,
            R.color.accent,
            R.color.status_pending,
            R.color.status_resolved,
            R.color.cat_hostel,
            R.color.cat_scholarship
        )
        var colorIdx = 0

        for ((cat, count) in dist) {
            val pct = (count * 100) / total
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
            }

            // Top Label & Count
            val headerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tvTitle = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = cat
                setTextColor(ContextCompat.getColor(this@AdminDashboardActivity, R.color.text_primary))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvCount = TextView(this).apply {
                text = "$count ($pct%)"
                setTextColor(ContextCompat.getColor(this@AdminDashboardActivity, R.color.text_secondary))
                textSize = 11f
            }

            headerLayout.addView(tvTitle)
            headerLayout.addView(tvCount)

            // Progress Bar
            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    14
                ).apply { setMargins(0, 6, 0, 0) }
                max = 100
                progress = pct
                isIndeterminate = false
                val color = ContextCompat.getColor(this@AdminDashboardActivity, colors[colorIdx % colors.size])
                progressTintList = android.content.res.ColorStateList.valueOf(color)
            }
            colorIdx++

            rowView.addView(headerLayout)
            rowView.addView(pb)
            layoutAnalyticsBars.addView(rowView)
        }
    }

    private fun filterComplaints() {
        val list = if (searchQuery.isNotEmpty()) {
            dbHelper.searchComplaints(searchQuery, currentFilter)
        } else {
            if (currentFilter == "All") dbHelper.getAllComplaints()
            else dbHelper.getComplaintsByStatus(currentFilter)
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

        tvTitle.text = "Grievance #${complaint.id}: ${complaint.title}"
        tvSubtitle.text = "Category: ${complaint.category} • Student: ${complaint.studentEmail ?: "Registered Student"}"

        when (complaint.status.lowercase()) {
            "resolved" -> dialogView.findViewById<RadioButton>(R.id.rb_resolved).isChecked = true
            "in progress" -> dialogView.findViewById<RadioButton>(R.id.rb_in_progress).isChecked = true
            else -> dialogView.findViewById<RadioButton>(R.id.rb_pending).isChecked = true
        }

        if (!complaint.officerReply.isNullOrEmpty()) {
            etReply.setText(complaint.officerReply)
        }

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
