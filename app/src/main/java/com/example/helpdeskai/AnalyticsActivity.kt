package com.example.helpdeskai

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager

class AnalyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        if (!sessionManager.getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)) {
            finish()
            return
        }
        setContentView(R.layout.activity_analytics)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_analytics)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val dbHelper = ComplaintDatabaseHelper(this)
        val allComplaints = dbHelper.getAllComplaints()
        val pending = allComplaints.count { it.status.equals("Pending", ignoreCase = true) }
        val inProgress = allComplaints.count { it.status.equals("In Progress", ignoreCase = true) }
        val resolved = allComplaints.count { it.status.equals("Resolved", ignoreCase = true) }

        // Student stats
        findViewById<TextView>(R.id.tv_analytics_total_students).text = dbHelper.getTotalStudentsCount().toString()
        findViewById<TextView>(R.id.tv_analytics_active_students).text = dbHelper.getActiveStudentsCount().toString()
        findViewById<TextView>(R.id.tv_analytics_new_reg).text = dbHelper.getNewRegistrationsCount().toString()

        // Complaint stats
        findViewById<TextView>(R.id.tv_analytics_total_complaints).text = allComplaints.size.toString()
        findViewById<TextView>(R.id.tv_analytics_pending).text = pending.toString()
        findViewById<TextView>(R.id.tv_analytics_in_progress).text = inProgress.toString()
        findViewById<TextView>(R.id.tv_analytics_resolved).text = resolved.toString()

        // Category bars
        val layoutBars = findViewById<LinearLayout>(R.id.layout_analytics_category_bars)
        val dist = dbHelper.getCategoryDistribution()
        val total = allComplaints.size
        val colors = listOf(R.color.primary, R.color.accent, R.color.status_pending, R.color.status_resolved, R.color.cat_hostel, R.color.cat_scholarship)
        var idx = 0

        for ((cat, count) in dist) {
            val pct = if (total > 0) (count * 100) / total else 0
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val tvCat = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = cat
                setTextColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_primary))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvCount2 = TextView(this).apply {
                text = "$count ($pct%)"
                setTextColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_secondary))
                textSize = 12f
            }
            header.addView(tvCat)
            header.addView(tvCount2)
            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16).apply { setMargins(0, 6, 0, 0) }
                max = 100; progress = pct; isIndeterminate = false
                progressTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this@AnalyticsActivity, colors[idx % colors.size]))
            }
            idx++
            row.addView(header); row.addView(pb)
            layoutBars.addView(row)
        }

        // Monthly breakdown
        val layoutMonthly = findViewById<LinearLayout>(R.id.layout_analytics_monthly)
        val monthly = dbHelper.getComplaintsByMonth()
        for ((month, count) in monthly) {
            val tv = TextView(this).apply {
                val unit = if (count == 1) "complaint" else "complaints"
                text = "$month: $count $unit"
                setTextColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.text_primary))
                textSize = 13f
                setPadding(0, 6, 0, 6)
            }
            layoutMonthly.addView(tv)
        }
    }
}
