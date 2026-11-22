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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.ComplaintAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class ComplaintListActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvComplaints: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnEmptyAction: MaterialButton
    private lateinit var fabNewComplaint: ExtendedFloatingActionButton
    private lateinit var cardStudentProfile: MaterialCardView
    private lateinit var cardProminentRaise: MaterialCardView
    private lateinit var btnRaiseProminent: MaterialButton
    private lateinit var chipGroupFilters: ChipGroup

    private lateinit var tvStudentNameBanner: TextView
    private lateinit var tvStudentIdBanner: TextView
    private lateinit var tvStudentTicketCount: TextView
    private lateinit var tvStudentAvatar: TextView

    private lateinit var tvStudentStatTotal: TextView
    private lateinit var tvStudentStatPending: TextView
    private lateinit var tvStudentStatProgress: TextView
    private lateinit var tvStudentStatResolved: TextView

    // Drawer Views
    private lateinit var tvDrawerAvatar: TextView
    private lateinit var tvDrawerUserName: TextView
    private lateinit var tvDrawerUserEmail: TextView

    private lateinit var adapter: ComplaintAdapter
    private var allComplaints: List<Complaint> = emptyList()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        // Protect route
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_complaint_list)

        initViews()
        setupToolbar()
        setupDrawerNavigation()
        setupRecyclerView()
        setupFilters()
        setupListeners()
        setupBackPressHandler()
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
        drawerLayout = findViewById(R.id.drawer_layout_student)
        rvComplaints = findViewById(R.id.rv_complaints)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        btnEmptyAction = findViewById(R.id.btn_empty_action)
        fabNewComplaint = findViewById(R.id.fab_new_complaint)
        cardStudentProfile = findViewById(R.id.card_student_profile)
        cardProminentRaise = findViewById(R.id.card_prominent_raise)
        btnRaiseProminent = findViewById(R.id.btn_raise_prominent)
        chipGroupFilters = findViewById(R.id.chip_group_student_filters)

        tvStudentNameBanner = findViewById(R.id.tv_student_name_banner)
        tvStudentIdBanner = findViewById(R.id.tv_student_id_banner)
        tvStudentTicketCount = findViewById(R.id.tv_student_ticket_count)
        tvStudentAvatar = findViewById(R.id.tv_student_avatar)

        tvStudentStatTotal = findViewById(R.id.tv_student_stat_total)
        tvStudentStatPending = findViewById(R.id.tv_student_stat_pending)
        tvStudentStatProgress = findViewById(R.id.tv_student_stat_progress)
        tvStudentStatResolved = findViewById(R.id.tv_student_stat_resolved)

        // Drawer Profile Bindings
        tvDrawerAvatar = findViewById(R.id.tv_drawer_avatar)
        tvDrawerUserName = findViewById(R.id.tv_drawer_user_name)
        tvDrawerUserEmail = findViewById(R.id.tv_drawer_user_email)

        cardStudentProfile.setOnClickListener {
            showProfileEditDialog()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_complaints)
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
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

    private fun setupDrawerNavigation() {
        findViewById<View>(R.id.nav_item_dashboard)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<View>(R.id.nav_item_raise)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ComplaintActivity::class.java))
        }
        findViewById<View>(R.id.nav_item_inbox)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, InboxActivity::class.java))
        }
        findViewById<View>(R.id.nav_item_profile)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showProfileEditDialog()
        }
        findViewById<View>(R.id.nav_item_faq)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showFaqDialog()
        }
        findViewById<View>(R.id.nav_item_logout)?.setOnClickListener {
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

    private fun setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentFilter = when (checkedIds[0]) {
                R.id.chip_student_pending -> "Pending"
                R.id.chip_student_progress -> "In Progress"
                R.id.chip_student_resolved -> "Resolved"
                else -> "All"
            }
            applyFilter()
        }
    }

    private fun showFaqDialog() {
        AlertDialog.Builder(this)
            .setTitle("Campus Support Guidelines & SLA")
            .setMessage(
                "• Infrastructure & Maintenance: Resolved within 48 business hours.\n\n" +
                "• Hostel & Mess Facilities: Addressed within 24 hours.\n\n" +
                "• Academics, Exams & Faculty: Processed within 72 business hours.\n\n" +
                "• IT Services & WiFi: Handled same day.\n\n" +
                "Official grievance decisions are reviewed by appointed college administration."
            )
            .setPositiveButton("Understood", null)
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
            // Card clicked - shows details or timeline status
            Toast.makeText(this, "Ticket #${complaint.id}: ${complaint.status}", Toast.LENGTH_SHORT).show()
        }
        rvComplaints.adapter = adapter
    }

    private fun setupListeners() {
        fabNewComplaint.setOnClickListener {
            startActivity(Intent(this, ComplaintActivity::class.java))
        }

        cardProminentRaise.setOnClickListener {
            startActivity(Intent(this, ComplaintActivity::class.java))
        }

        btnRaiseProminent.setOnClickListener {
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

            // Drawer Bindings
            tvDrawerUserName.text = user.name
            tvDrawerUserEmail.text = user.email

            // Initials avatar
            val initials = user.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            val avatarStr = if (initials.isNotEmpty()) initials else "S"
            tvStudentAvatar.text = avatarStr
            tvDrawerAvatar.text = avatarStr
        }

        val userEmail = user?.email
        allComplaints = if (!userEmail.isNullOrEmpty()) {
            dbHelper.getComplaintsByStudent(userEmail)
        } else {
            dbHelper.getAllComplaints()
        }

        tvStudentTicketCount.text = "${allComplaints.size} ${if (allComplaints.size == 1) "Ticket" else "Tickets"}"

        // Populate 4 SaaS metric cards
        val pending = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val inProgress = allComplaints.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolved = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        tvStudentStatTotal.text = allComplaints.size.toString()
        tvStudentStatPending.text = pending.toString()
        tvStudentStatProgress.text = inProgress.toString()
        tvStudentStatResolved.text = resolved.toString()

        applyFilter()
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "Pending" -> allComplaints.filter { it.status.equals("Pending", ignoreCase = true) }
            "In Progress" -> allComplaints.filter { it.status.equals("In Progress", ignoreCase = true) }
            "Resolved" -> allComplaints.filter { it.status.equals("Resolved", ignoreCase = true) }
            else -> allComplaints
        }

        if (filteredList.isEmpty()) {
            rvComplaints.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvComplaints.visibility = View.VISIBLE
            adapter.updateList(filteredList)
        }
    }
}
