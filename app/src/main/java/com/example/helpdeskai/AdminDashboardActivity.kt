package com.example.helpdeskai

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.card.MaterialCardView

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

    private lateinit var rvAdminComplaints: RecyclerView
    private lateinit var layoutAdminEmptyState: LinearLayout
    private lateinit var adapter: ComplaintAdapter

    private lateinit var tvAdminDrawerName: TextView
    private lateinit var tvAdminDrawerEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        if (!sessionManager.getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)) {
            Toast.makeText(this, "Access Denied: Support account required.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ComplaintListActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_admin_dashboard)

        initViews()
        setupToolbar()
        setupDrawerNavigation()
        setupRecyclerView()
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

        rvAdminComplaints = findViewById(R.id.rv_admin_complaints)
        layoutAdminEmptyState = findViewById(R.id.layout_admin_empty_state)

        tvAdminDrawerName = findViewById(R.id.tv_admin_drawer_name)
        tvAdminDrawerEmail = findViewById(R.id.tv_admin_drawer_email)

        cardAdminProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.tv_view_all_complaints)?.setOnClickListener {
            startActivity(Intent(this, AdminComplaintsActivity::class.java))
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
        findViewById<View>(R.id.nav_admin_item_complaints)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AdminComplaintsActivity::class.java))
        }
        findViewById<View>(R.id.nav_admin_item_students)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, StudentsActivity::class.java))
        }
        findViewById<View>(R.id.nav_admin_item_analytics)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        findViewById<View>(R.id.nav_admin_item_notifications)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, InboxActivity::class.java))
        }
        findViewById<View>(R.id.nav_admin_item_settings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ProfileActivity::class.java))
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
        rvAdminComplaints.layoutManager = LinearLayoutManager(this)
        adapter = ComplaintAdapter(emptyList()) { complaint ->
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("COMPLAINT_ID", complaint.id)
            startActivity(intent)
        }
        rvAdminComplaints.adapter = adapter
    }

    private fun refreshDashboard() {
        val user = sessionManager.getUser()
        tvAdminWelcomeName.text = "Hello, Support Team"
        tvAdminEmailSub.text = "${user?.email ?: "admin@gmail.com"} • Support"

        tvAdminDrawerName.text = "Support Team"
        tvAdminDrawerEmail.text = user?.email ?: "admin@gmail.com"

        val allComplaints = dbHelper.getAllComplaints()
        val pendingCount = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val progressCount = allComplaints.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolvedCount = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        // Animate count-ups
        tvStatTotal.text = allComplaints.size.toString()
        tvStatPending.text = pendingCount.toString()
        tvStatProgress.text = progressCount.toString()
        tvStatResolved.text = resolvedCount.toString()

        // "Needs your attention" list: pending complaints first, then in-progress
        val attentionList = allComplaints
            .filter { !it.status.equals("Resolved", ignoreCase = true) }
            .sortedBy { if (it.status.equals("Pending", ignoreCase = true)) 0 else 1 }

        if (attentionList.isEmpty()) {
            rvAdminComplaints.visibility = View.GONE
            layoutAdminEmptyState.visibility = View.VISIBLE
        } else {
            layoutAdminEmptyState.visibility = View.GONE
            rvAdminComplaints.visibility = View.VISIBLE
            adapter.updateList(attentionList)
        }
    }
}
