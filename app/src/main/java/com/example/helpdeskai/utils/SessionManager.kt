package com.example.helpdeskai.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.helpdeskai.data.model.User

/**
 * Session Manager to track currently logged-in user using SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "helpdesk_ai_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_USER_ROLE = "user_role"
    }

    fun saveUserSession(user: User) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_STUDENT_ID, user.studentId)
            putString(KEY_USER_ROLE, user.role)
            apply()
        }
    }

    fun updateUserProfile(newName: String, newStudentId: String?) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, newName)
            if (newStudentId != null) {
                putString(KEY_STUDENT_ID, newStudentId)
            }
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUser(): User? {
        if (!isLoggedIn()) return null
        return User(
            id = prefs.getLong(KEY_USER_ID, 0L),
            name = prefs.getString(KEY_USER_NAME, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            password = "",
            studentId = prefs.getString(KEY_STUDENT_ID, null),
            role = prefs.getString(KEY_USER_ROLE, User.ROLE_STUDENT) ?: User.ROLE_STUDENT
        )
    }

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, User.ROLE_STUDENT) ?: User.ROLE_STUDENT

    fun isAdmin(): Boolean = getUserRole().equals(User.ROLE_ADMIN, ignoreCase = true)

    fun logout() {
        prefs.edit().clear().apply()
    }
}
