package com.example.helpdeskai.data.model

import java.io.Serializable

/**
 * Model representing a Complaint / Grievance ticket.
 */
data class Complaint(
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // Hostel & Mess, Scholarship & Fees, Academics & Exams, etc.
    val photoPath: String? = null,
    val locationAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "Pending", // Pending, In Progress, Resolved
    val officerReply: String? = null,
    val studentEmail: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
