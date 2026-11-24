package com.example.helpdeskai

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.CameraHelper
import com.example.helpdeskai.utils.GroqAiService
import com.example.helpdeskai.utils.NotificationHelper
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComplaintDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var complaintId: Long = -1L
    private var complaint: Complaint? = null
    private var isAdmin: Boolean = false

    // Header Views
    private lateinit var tvCat: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvTime: TextView
    private lateinit var ivPhoto: ImageView
    private lateinit var tvLocation: TextView

    // Tracker Views
    private lateinit var dot1: ImageView
    private lateinit var line1: View
    private lateinit var dot2: ImageView
    private lateinit var line2: View
    private lateinit var dot3: ImageView
    private lateinit var line3: View
    private lateinit var dot4: ImageView
    private lateinit var label1: TextView
    private lateinit var label2: TextView
    private lateinit var label3: TextView
    private lateinit var label4: TextView

    // Feedback Views
    private lateinit var cardFeedback: MaterialCardView
    private lateinit var btnFeedbackYes: MaterialButton
    private lateinit var btnFeedbackNo: MaterialButton
    private lateinit var layoutStarRating: LinearLayout
    private lateinit var tvFeedbackThanks: TextView
    private lateinit var layoutYesNo: LinearLayout

    // Chat Views
    private lateinit var tvChatStudentHeader: TextView
    private lateinit var tvChatStudentText: TextView
    private lateinit var tvChatStudentTime: TextView
    private lateinit var layoutChatOfficerBubble: LinearLayout
    private lateinit var tvChatOfficerText: TextView
    private lateinit var tvChatOfficerTime: TextView

    // Admin Reply Views
    private lateinit var layoutDetailMainContent: View
    private lateinit var layoutReplySuccess: View
    private lateinit var btnBackToComplaints: MaterialButton
    private lateinit var layoutAdminReplySection: LinearLayout
    private lateinit var spinnerStatus: Spinner
    private lateinit var etReplyText: EditText
    private lateinit var btnAiWriteReply: MaterialButton
    private lateinit var btnSendReply: MaterialButton

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_detail)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)
        complaintId = intent.getLongExtra("COMPLAINT_ID", -1L)
        isAdmin = sessionManager.getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)

        initViews()
        loadComplaintData()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvCat = findViewById(R.id.tv_detail_cat)
        tvStatusBadge = findViewById(R.id.tv_detail_status_badge)
        tvTitle = findViewById(R.id.tv_detail_title)
        tvTime = findViewById(R.id.tv_detail_time)
        ivPhoto = findViewById(R.id.iv_detail_photo)
        tvLocation = findViewById(R.id.tv_detail_location)

        dot1 = findViewById(R.id.detail_dot_1)
        line1 = findViewById(R.id.detail_line_1)
        dot2 = findViewById(R.id.detail_dot_2)
        line2 = findViewById(R.id.detail_line_2)
        dot3 = findViewById(R.id.detail_dot_3)
        line3 = findViewById(R.id.detail_line_3)
        dot4 = findViewById(R.id.detail_dot_4)
        label1 = findViewById(R.id.detail_tv_label_1)
        label2 = findViewById(R.id.detail_tv_label_2)
        label3 = findViewById(R.id.detail_tv_label_3)
        label4 = findViewById(R.id.detail_tv_label_4)

        cardFeedback = findViewById(R.id.card_resolved_feedback)
        btnFeedbackYes = findViewById(R.id.btn_feedback_yes)
        btnFeedbackNo = findViewById(R.id.btn_feedback_no)
        layoutStarRating = findViewById(R.id.layout_star_rating)
        tvFeedbackThanks = findViewById(R.id.tv_feedback_thanks)
        layoutYesNo = findViewById(R.id.layout_yes_no)

        tvChatStudentHeader = findViewById(R.id.tv_chat_student_header)
        tvChatStudentText = findViewById(R.id.tv_chat_student_text)
        tvChatStudentTime = findViewById(R.id.tv_chat_student_time)
        layoutChatOfficerBubble = findViewById(R.id.layout_chat_officer_bubble)
        tvChatOfficerText = findViewById(R.id.tv_chat_officer_text)
        tvChatOfficerTime = findViewById(R.id.tv_chat_officer_time)

        layoutDetailMainContent = findViewById(R.id.layout_detail_main_content)
        layoutReplySuccess = findViewById(R.id.layout_reply_success)
        btnBackToComplaints = findViewById(R.id.btn_back_to_complaints)
        layoutAdminReplySection = findViewById(R.id.layout_admin_reply_section)
        spinnerStatus = findViewById(R.id.spinner_status_update)
        etReplyText = findViewById(R.id.et_admin_reply_text)
        btnAiWriteReply = findViewById(R.id.btn_ai_write_reply)
        btnSendReply = findViewById(R.id.btn_send_reply)

        // Status Spinner for Admin with custom high-contrast styling and colored dot
        val statusList = listOf("Pending", "In Progress", "Resolved")
        val statusAdapter = StatusSpinnerAdapter(this, statusList)
        spinnerStatus.adapter = statusAdapter

        // Quick Reply Chips
        findViewById<View>(R.id.chip_reply_checking)?.setOnClickListener {
            etReplyText.setText("We are checking this issue with our campus team.")
            spinnerStatus.setSelection(1) // In Progress
        }
        findViewById<View>(R.id.chip_reply_fixed)?.setOnClickListener {
            etReplyText.setText("Fixed! Please verify and confirm if the issue is resolved.")
            spinnerStatus.setSelection(2) // Resolved
        }
        findViewById<View>(R.id.chip_reply_details)?.setOnClickListener {
            etReplyText.setText("We need more details to resolve this. Please provide specific room or equipment number.")
            spinnerStatus.setSelection(1) // In Progress
        }

        // Write with AI
        btnAiWriteReply.setOnClickListener {
            val c = complaint ?: return@setOnClickListener
            btnAiWriteReply.isEnabled = false
            btnAiWriteReply.text = "Writing..."
            GroqAiService.generateAdminReply(
                complaintTitle = c.title,
                complaintDescription = c.description,
                complaintCategory = c.category,
                onSuccess = { reply ->
                    btnAiWriteReply.isEnabled = true
                    btnAiWriteReply.text = "✨ Write with AI"
                    etReplyText.setText(reply)
                    spinnerStatus.setSelection(1)
                    Toast.makeText(this, "AI drafted reply!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    btnAiWriteReply.isEnabled = true
                    btnAiWriteReply.text = "✨ Write with AI"
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Send Reply
        btnSendReply.setOnClickListener {
            val reply = etReplyText.text.toString().trim()
            val newStatus = spinnerStatus.selectedItem?.toString() ?: "In Progress"
            val c = complaint ?: return@setOnClickListener

            val rows = dbHelper.updateStatusAndReply(c.id, newStatus, if (reply.isNotEmpty()) reply else null)
            if (rows > 0) {
                NotificationHelper.sendStatusUpdateNotification(
                    this,
                    c.id,
                    c.title,
                    newStatus,
                    reply
                )
                layoutDetailMainContent.visibility = View.GONE
                layoutReplySuccess.visibility = View.VISIBLE
                btnBackToComplaints.setOnClickListener {
                    finish()
                }
            } else {
                Toast.makeText(this, "Failed to send reply. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Feedback Buttons for Students
        btnFeedbackYes.setOnClickListener {
            layoutYesNo.visibility = View.GONE
            layoutStarRating.visibility = View.VISIBLE
        }
        btnFeedbackNo.setOnClickListener {
            layoutYesNo.visibility = View.GONE
            tvFeedbackThanks.text = "We notified support that this issue still needs attention."
            tvFeedbackThanks.visibility = View.VISIBLE
            complaint?.let { c ->
                dbHelper.updateStatusAndReply(c.id, "In Progress", c.officerReply)
                loadComplaintData()
            }
        }

        // Star clicks
        val stars = listOf(
            findViewById<TextView>(R.id.star_1),
            findViewById<TextView>(R.id.star_2),
            findViewById<TextView>(R.id.star_3),
            findViewById<TextView>(R.id.star_4),
            findViewById<TextView>(R.id.star_5)
        )
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                layoutStarRating.visibility = View.GONE
                tvFeedbackThanks.text = "Thank you for rating ${index + 1} stars! ⭐"
                tvFeedbackThanks.visibility = View.VISIBLE
            }
        }

        // Photo tap to enlarge
        ivPhoto.setOnClickListener {
            complaint?.photoPath?.let { path ->
                val bitmap = CameraHelper.loadBitmapFromPath(path)
                if (bitmap != null) {
                    val imgView = ImageView(this).apply {
                        setImageBitmap(bitmap)
                        adjustViewBounds = true
                    }
                    AlertDialog.Builder(this)
                        .setView(imgView)
                        .setPositiveButton("Close", null)
                        .show()
                }
            }
        }
    }

    private fun loadComplaintData() {
        val all = dbHelper.getAllComplaints()
        complaint = all.find { it.id == complaintId }
        val c = complaint ?: return

        tvCat.text = c.category
        tvCat.setTextColor(ContextCompat.getColor(this, R.color.white))
        tvTitle.text = c.title
        tvTime.text = "Submitted: ${dateFormat.format(Date(c.timestamp))}"

        // Status Badge with high-contrast white text
        tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.white))
        when (c.status.lowercase()) {
            "resolved" -> {
                tvStatusBadge.text = "Resolved"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_resolved)
            }
            "in progress" -> {
                tvStatusBadge.text = "In Progress"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_progress)
            }
            else -> {
                tvStatusBadge.text = "Pending"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            }
        }

        // Tracker: ONLY steps reached green
        val colorResolved = ContextCompat.getColor(this, R.color.status_resolved)
        val colorMuted = ContextCompat.getColor(this, R.color.text_muted)

        when (c.status.lowercase()) {
            "resolved" -> {
                dot1.setImageResource(R.drawable.dot_timeline_resolved)
                line1.setBackgroundResource(R.drawable.line_timeline_active)
                dot2.setImageResource(R.drawable.dot_timeline_resolved)
                line2.setBackgroundResource(R.drawable.line_timeline_active)
                dot3.setImageResource(R.drawable.dot_timeline_resolved)
                line3.setBackgroundResource(R.drawable.line_timeline_active)
                dot4.setImageResource(R.drawable.dot_timeline_resolved)
                label1.setTextColor(colorResolved)
                label2.setTextColor(colorResolved)
                label3.setTextColor(colorResolved)
                label4.setTextColor(colorResolved)
            }
            "in progress" -> {
                dot1.setImageResource(R.drawable.dot_timeline_resolved)
                line1.setBackgroundResource(R.drawable.line_timeline_active)
                dot2.setImageResource(R.drawable.dot_timeline_resolved)
                line2.setBackgroundResource(R.drawable.line_timeline_active)
                dot3.setImageResource(R.drawable.dot_timeline_resolved)
                line3.setBackgroundResource(R.drawable.line_timeline_inactive)
                dot4.setImageResource(R.drawable.dot_timeline_inactive)
                label1.setTextColor(colorResolved)
                label2.setTextColor(colorResolved)
                label3.setTextColor(colorResolved)
                label4.setTextColor(colorMuted)
            }
            else -> {
                dot1.setImageResource(R.drawable.dot_timeline_resolved)
                line1.setBackgroundResource(R.drawable.line_timeline_inactive)
                dot2.setImageResource(R.drawable.dot_timeline_inactive)
                line2.setBackgroundResource(R.drawable.line_timeline_inactive)
                dot3.setImageResource(R.drawable.dot_timeline_inactive)
                line3.setBackgroundResource(R.drawable.line_timeline_inactive)
                dot4.setImageResource(R.drawable.dot_timeline_inactive)
                label1.setTextColor(colorResolved)
                label2.setTextColor(colorMuted)
                label3.setTextColor(colorMuted)
                label4.setTextColor(colorMuted)
            }
        }

        // Photo
        if (!c.photoPath.isNullOrEmpty()) {
            val bitmap = CameraHelper.loadBitmapFromPath(c.photoPath)
            if (bitmap != null) {
                ivPhoto.setImageBitmap(bitmap)
                ivPhoto.visibility = View.VISIBLE
            }
        } else {
            ivPhoto.visibility = View.GONE
        }

        // Location
        if (!c.locationAddress.isNullOrEmpty()) {
            tvLocation.text = "📍 Location: ${c.locationAddress}"
            tvLocation.visibility = View.VISIBLE
        } else {
            tvLocation.visibility = View.GONE
        }

        // Conversation: Student name (not email) and capitalized
        val email = c.studentEmail ?: ""
        val studentUser = if (email.isNotEmpty()) dbHelper.getStudentByEmail(email) else null
        val rawName = studentUser?.name ?: if (email.isNotEmpty()) email else "Student"
        val studentDisplayName = rawName
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        tvChatStudentHeader.text = studentDisplayName
        tvChatStudentText.text = c.description
        tvChatStudentTime.text = dateFormat.format(Date(c.timestamp))

        // Conversation: Officer reply
        if (!c.officerReply.isNullOrBlank()) {
            tvChatOfficerText.text = c.officerReply
            layoutChatOfficerBubble.visibility = View.VISIBLE
        } else {
            layoutChatOfficerBubble.visibility = View.GONE
        }

        // Role-based visibility
        if (isAdmin) {
            layoutAdminReplySection.visibility = View.VISIBLE
            cardFeedback.visibility = View.GONE
            when (c.status.lowercase()) {
                "resolved" -> spinnerStatus.setSelection(2)
                "in progress" -> spinnerStatus.setSelection(1)
                else -> spinnerStatus.setSelection(0)
            }
        } else {
            layoutAdminReplySection.visibility = View.GONE
            // Show feedback card only if resolved
            if (c.status.equals("Resolved", ignoreCase = true)) {
                cardFeedback.visibility = View.VISIBLE
            } else {
                cardFeedback.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isAdmin) {
            menuInflater.inflate(R.menu.menu_complaint_detail, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete_complaint) {
            AlertDialog.Builder(this)
                .setTitle("Delete Complaint")
                .setMessage("Are you sure you want to delete this complaint?")
                .setPositiveButton("Delete") { _, _ ->
                    val c = complaint ?: return@setPositiveButton
                    dbHelper.deleteComplaint(c.id)
                    val root = findViewById<View>(android.R.id.content)
                    com.google.android.material.snackbar.Snackbar.make(
                        root,
                        "Complaint deleted",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("UNDO") {
                        dbHelper.restoreComplaint(c)
                        loadComplaintData()
                    }.addCallback(object : com.google.android.material.snackbar.Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: com.google.android.material.snackbar.Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                finish()
                            }
                        }
                    }).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class StatusSpinnerAdapter(
        context: Context,
        private val items: List<String>
    ) : ArrayAdapter<String>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_status_spinner_selected, parent, false)
            val item = items[position]

            val tvText = view.findViewById<TextView>(R.id.tv_status_text)
            val dot = view.findViewById<View>(R.id.view_status_dot)

            tvText.text = item
            when (item.lowercase()) {
                "resolved" -> dot.setBackgroundResource(R.drawable.dot_status_resolved)
                "in progress" -> dot.setBackgroundResource(R.drawable.dot_status_progress)
                else -> dot.setBackgroundResource(R.drawable.dot_status_pending)
            }
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_status_spinner_dropdown, parent, false)
            val item = items[position]

            val tvText = view.findViewById<TextView>(R.id.tv_dropdown_text)
            val dot = view.findViewById<View>(R.id.view_dropdown_dot)
            val ivCheck = view.findViewById<ImageView>(R.id.iv_dropdown_check)
            val container = view.findViewById<View>(R.id.layout_dropdown_item)

            tvText.text = item
            when (item.lowercase()) {
                "resolved" -> dot.setBackgroundResource(R.drawable.dot_status_resolved)
                "in progress" -> dot.setBackgroundResource(R.drawable.dot_status_progress)
                else -> dot.setBackgroundResource(R.drawable.dot_status_pending)
            }

            val isSelected = position == spinnerStatus.selectedItemPosition
            if (isSelected) {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_card_alt))
                ivCheck.visibility = View.VISIBLE
            } else {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_card))
                ivCheck.visibility = View.GONE
            }

            return view
        }
    }
}
