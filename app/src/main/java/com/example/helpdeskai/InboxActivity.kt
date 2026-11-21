package com.example.helpdeskai

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.data.InboxAdapter
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.InboxMessage
import com.example.helpdeskai.utils.SessionManager

class InboxActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var rvInbox: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: InboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        initViews()
        loadInboxMessages()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_inbox)
        toolbar.setNavigationOnClickListener { finish() }

        rvInbox = findViewById(R.id.rv_inbox)
        layoutEmpty = findViewById(R.id.layout_inbox_empty)

        rvInbox.layoutManager = LinearLayoutManager(this)
        adapter = InboxAdapter(emptyList())
        rvInbox.adapter = adapter
    }

    private fun loadInboxMessages() {
        val userEmail = sessionManager.getUser()?.email
        val complaints = if (!userEmail.isNullOrEmpty()) {
            dbHelper.getComplaintsByStudent(userEmail)
        } else {
            dbHelper.getAllComplaints()
        }

        val messages = mutableListOf<InboxMessage>()

        for (c in complaints) {
            // If officer replied
            if (!c.officerReply.isNullOrBlank()) {
                messages.add(
                    InboxMessage(
                        id = c.id * 10 + 2,
                        title = "Officer Replied to Ticket #${c.id}",
                        body = "Officer Note: ${c.officerReply}",
                        tag = "OFFICIAL REPLY",
                        timestamp = c.timestamp + 60000,
                        isUnread = true
                    )
                )
            }

            // If status is updated
            if (!c.status.equals("Pending", ignoreCase = true)) {
                messages.add(
                    InboxMessage(
                        id = c.id * 10 + 1,
                        title = "Ticket #${c.id} marked as ${c.status.uppercase()}",
                        body = "\"${c.title}\" has been updated to ${c.status}.",
                        tag = "STATUS UPDATE",
                        timestamp = c.timestamp + 30000,
                        isUnread = c.status.equals("Resolved", ignoreCase = true)
                    )
                )
            }

            // Complaint filed
            messages.add(
                InboxMessage(
                    id = c.id * 10,
                    title = "Grievance #${c.id} Submitted",
                    body = "\"${c.title}\" received and assigned to ${c.category} department.",
                    tag = "SUBMITTED",
                    timestamp = c.timestamp,
                    isUnread = false
                )
            )
        }

        messages.sortByDescending { it.timestamp }

        val toolbar = findViewById<Toolbar>(R.id.toolbar_inbox)
        toolbar.subtitle = "${messages.size} Messages"

        if (messages.isEmpty()) {
            rvInbox.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            rvInbox.visibility = View.VISIBLE
            adapter.updateList(messages)
        }
    }
}
