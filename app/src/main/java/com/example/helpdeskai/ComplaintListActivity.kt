package com.example.helpdeskai

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
import java.util.Locale

class ComplaintListActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvComplaints: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var cardStudentProfile: MaterialCardView
    private lateinit var btnRaiseProminent: MaterialButton

    private lateinit var tvStudentNameBanner: TextView
    private lateinit var tvStudentIdBanner: TextView
    private lateinit var tvStudentAvatar: TextView

    private lateinit var tvStudentStatPending: TextView
    private lateinit var tvStudentStatProgress: TextView
    private lateinit var tvStudentStatResolved: TextView

    // Drawer Views
    private lateinit var tvDrawerAvatar: TextView
    private lateinit var tvDrawerUserName: TextView
    private lateinit var tvDrawerUserEmail: TextView

    private lateinit var adapter: ComplaintAdapter
    private var allComplaints: List<Complaint> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

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
        cardStudentProfile = findViewById(R.id.card_student_profile)
        btnRaiseProminent = findViewById(R.id.btn_raise_prominent)

        tvStudentNameBanner = findViewById(R.id.tv_student_name_banner)
        tvStudentIdBanner = findViewById(R.id.tv_student_id_banner)
        tvStudentAvatar = findViewById(R.id.tv_student_avatar)

        tvStudentStatPending = findViewById(R.id.tv_student_stat_pending)
        tvStudentStatProgress = findViewById(R.id.tv_student_stat_progress)
        tvStudentStatResolved = findViewById(R.id.tv_student_stat_resolved)

        tvDrawerAvatar = findViewById(R.id.tv_drawer_avatar)
        tvDrawerUserName = findViewById(R.id.tv_drawer_user_name)
        tvDrawerUserEmail = findViewById(R.id.tv_drawer_user_email)

        cardStudentProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.tv_view_all_student)?.setOnClickListener {
            startActivity(Intent(this, MyComplaintsActivity::class.java))
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
    }

    private fun setupDrawerNavigation() {
        findViewById<View>(R.id.nav_item_my_complaints)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, MyComplaintsActivity::class.java))
        }
        findViewById<View>(R.id.nav_item_inbox)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, InboxActivity::class.java))
        }
        findViewById<View>(R.id.nav_item_profile)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.nav_item_faq)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, GuidelinesActivity::class.java))
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

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
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
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("COMPLAINT_ID", complaint.id)
            startActivity(intent)
        }
        rvComplaints.adapter = adapter
    }

    private fun setupListeners() {
        btnRaiseProminent.setOnClickListener {
            startActivity(Intent(this, ComplaintActivity::class.java))
        }
    }

    private fun loadComplaints() {
        val user = sessionManager.getUser()
        if (user != null) {
            val capitalizedName = user.name.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            tvStudentNameBanner.text = "Hello, $capitalizedName"
            val idDisplay = if (!user.studentId.isNullOrEmpty()) user.studentId else "Student"
            tvStudentIdBanner.text = "ID: $idDisplay • Tap to view profile"

            tvDrawerUserName.text = capitalizedName
            tvDrawerUserEmail.text = user.email

            val initials = capitalizedName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            val avatarStr = if (initials.isNotEmpty()) initials else "S"
            tvStudentAvatar.text = avatarStr
            tvDrawerAvatar.text = avatarStr
        }

        val userEmail = user?.email
        allComplaints = if (!userEmail.isNullOrEmpty()) {
            dbHelper.getComplaintsByStudent(userEmail)
        } else {
            emptyList()
        }

        val pending = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val inProgress = allComplaints.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolved = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        tvStudentStatPending.text = pending.toString()
        tvStudentStatProgress.text = inProgress.toString()
        tvStudentStatResolved.text = resolved.toString()

        // Display latest complaints (top 5) on the home screen
        val latestList = allComplaints.take(5)

        if (latestList.isEmpty()) {
            rvComplaints.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvComplaints.visibility = View.VISIBLE
            adapter.updateList(latestList)
        }
    }
}
