package com.example.helpdeskai.data.model

/**
 * User entity representing either a Student or an Administrative Officer.
 */
data class User(
    val id: Long = 0,
    val name: String,
    val email: String,
    val password: String,
    val studentId: String? = null,
    val role: String = ROLE_STUDENT, // "STUDENT" or "ADMIN"
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_STUDENT = "STUDENT"
        const val ROLE_ADMIN = "ADMIN"
    }

    val isAdmin: Boolean
        get() = role.equals(ROLE_ADMIN, ignoreCase = true)
}

