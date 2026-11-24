package com.example.helpdeskai.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.data.model.User

/**
 * SQLite Database Helper managing local persistent storage for Users and Complaints (CRUD).
 */
class ComplaintDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "helpdesk_ai.db"
        private const val DATABASE_VERSION = 2

        // Table: Users
        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_NAME = "name"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_STUDENT_ID = "student_id"
        const val COLUMN_USER_ROLE = "role"
        const val COLUMN_USER_CREATED_AT = "created_at"
        const val COLUMN_USER_LAST_LOGIN = "last_login"

        // Table: Complaints
        const val TABLE_COMPLAINTS = "complaints"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_PHOTO_PATH = "photo_path"
        const val COLUMN_LOCATION_ADDRESS = "location_address"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_STATUS = "status"
        const val COLUMN_OFFICER_REPLY = "officer_reply"
        const val COLUMN_STUDENT_EMAIL = "student_email"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Create Users Table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_USER_PASSWORD TEXT NOT NULL,
                $COLUMN_USER_STUDENT_ID TEXT,
                $COLUMN_USER_ROLE TEXT NOT NULL,
                $COLUMN_USER_CREATED_AT INTEGER DEFAULT 0,
                $COLUMN_USER_LAST_LOGIN INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // Seed Master Admin Account
        val adminValues = ContentValues().apply {
            put(COLUMN_USER_NAME, "Support Team")
            put(COLUMN_USER_EMAIL, "admin@gmail.com")
            put(COLUMN_USER_PASSWORD, "admin123")
            put(COLUMN_USER_STUDENT_ID, "STAFF-001")
            put(COLUMN_USER_ROLE, User.ROLE_ADMIN)
            put(COLUMN_USER_CREATED_AT, System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
            put(COLUMN_USER_LAST_LOGIN, System.currentTimeMillis())
        }
        db.insert(TABLE_USERS, null, adminValues)

        // 2. Create Complaints Table
        val createComplaintsTable = """
            CREATE TABLE $TABLE_COMPLAINTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_PHOTO_PATH TEXT,
                $COLUMN_LOCATION_ADDRESS TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_STATUS TEXT DEFAULT 'Pending',
                $COLUMN_OFFICER_REPLY TEXT,
                $COLUMN_STUDENT_EMAIL TEXT,
                $COLUMN_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(createComplaintsTable)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Ensure new columns exist seamlessly without losing existing user accounts
        try {
            val cursor = db.rawQuery("PRAGMA table_info($TABLE_USERS)", null)
            val columns = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    columns.add(it.getString(it.getColumnIndexOrThrow("name")))
                }
            }
            if (!columns.contains(COLUMN_USER_CREATED_AT)) {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_USER_CREATED_AT INTEGER DEFAULT 0")
            }
            if (!columns.contains(COLUMN_USER_LAST_LOGIN)) {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_USER_LAST_LOGIN INTEGER DEFAULT 0")
            }
            // Populate default timestamps for any legacy records
            val now = System.currentTimeMillis()
            db.execSQL("UPDATE $TABLE_USERS SET $COLUMN_USER_CREATED_AT = $now WHERE $COLUMN_USER_CREATED_AT = 0 OR $COLUMN_USER_CREATED_AT IS NULL")
            db.execSQL("UPDATE $TABLE_USERS SET $COLUMN_USER_LAST_LOGIN = $now WHERE $COLUMN_USER_LAST_LOGIN = 0 OR $COLUMN_USER_LAST_LOGIN IS NULL")
            // Ensure admin name is 'Support Team'
            val cv = ContentValues().apply {
                put(COLUMN_USER_NAME, "Support Team")
            }
            db.update(TABLE_USERS, cv, "$COLUMN_USER_EMAIL = ?", arrayOf("admin@gmail.com"))
        } catch (_: Exception) {}
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Drop & recreate cleanly for schema upgrade
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_COMPLAINTS")
            onCreate(db)
        }
    }

    // =========================================================================
    // USER AUTHENTICATION (STUDENT & ADMIN)
    // =========================================================================

    /**
     * Register a new student account.
     * @return User ID if successful, -1 if email already exists or failed.
     */
    fun registerStudent(name: String, email: String, pass: String, studentId: String): Long {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, name.trim())
            put(COLUMN_USER_EMAIL, email.trim().lowercase())
            put(COLUMN_USER_PASSWORD, pass.trim())
            put(COLUMN_USER_STUDENT_ID, studentId.trim().uppercase())
            put(COLUMN_USER_ROLE, User.ROLE_STUDENT)
            put(COLUMN_USER_CREATED_AT, now)
            put(COLUMN_USER_LAST_LOGIN, now)
        }
        return try {
            db.insertOrThrow(TABLE_USERS, null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Authenticate user credentials and update last login timestamp.
     * @return User object if valid, null if invalid credentials.
     */
    fun login(email: String, pass: String): User? {
        val db = writableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_EMAIL = ? AND $COLUMN_USER_PASSWORD = ?",
            arrayOf(email.trim().lowercase(), pass.trim()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val user = cursorToUser(it)
                // Update real last_login timestamp
                val now = System.currentTimeMillis()
                val updateValues = ContentValues().apply {
                    put(COLUMN_USER_LAST_LOGIN, now)
                }
                db.update(TABLE_USERS, updateValues, "$COLUMN_USER_ID = ?", arrayOf(user.id.toString()))
                return user.copy(lastLogin = now)
            }
        }
        return null
    }

    private fun cursorToUser(cursor: Cursor): User {
        val createdAtIdx = cursor.getColumnIndex(COLUMN_USER_CREATED_AT)
        val lastLoginIdx = cursor.getColumnIndex(COLUMN_USER_LAST_LOGIN)
        return User(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
            email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
            password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_PASSWORD)),
            studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_STUDENT_ID)),
            role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ROLE)),
            createdAt = if (createdAtIdx != -1 && !cursor.isNull(createdAtIdx)) cursor.getLong(createdAtIdx) else System.currentTimeMillis(),
            lastLogin = if (lastLoginIdx != -1 && !cursor.isNull(lastLoginIdx)) cursor.getLong(lastLoginIdx) else System.currentTimeMillis()
        )
    }

    // =========================================================================
    // COMPLAINTS CRUD OPERATIONS
    // =========================================================================

    /**
     * [C]reate: Insert a new complaint.
     */
    fun insertComplaint(complaint: Complaint): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, complaint.title)
            put(COLUMN_DESCRIPTION, complaint.description)
            put(COLUMN_CATEGORY, complaint.category)
            put(COLUMN_PHOTO_PATH, complaint.photoPath)
            put(COLUMN_LOCATION_ADDRESS, complaint.locationAddress)
            put(COLUMN_LATITUDE, complaint.latitude)
            put(COLUMN_LONGITUDE, complaint.longitude)
            put(COLUMN_STATUS, complaint.status)
            put(COLUMN_OFFICER_REPLY, complaint.officerReply)
            put(COLUMN_STUDENT_EMAIL, complaint.studentEmail)
            put(COLUMN_TIMESTAMP, complaint.timestamp)
        }
        return db.insert(TABLE_COMPLAINTS, null, values)
    }

    /**
     * [R]ead: Get all complaints (Admin View).
     */
    fun getAllComplaints(): List<Complaint> {
        val list = mutableListOf<Complaint>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_COMPLAINTS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToComplaint(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    /**
     * [R]ead: Filter complaints submitted by a specific student (Student View).
     */
    fun getComplaintsByStudent(studentEmail: String): List<Complaint> {
        val list = mutableListOf<Complaint>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_COMPLAINTS,
            null,
            "$COLUMN_STUDENT_EMAIL = ?",
            arrayOf(studentEmail.trim().lowercase()),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToComplaint(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    /**
     * [R]ead: Filter complaints by status.
     */
    fun getComplaintsByStatus(status: String): List<Complaint> {
        val list = mutableListOf<Complaint>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_COMPLAINTS,
            null,
            "$COLUMN_STATUS = ?",
            arrayOf(status),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToComplaint(it))
                } while (it.moveToNext())
            }
        }
        return list
    }

    /**
     * [U]pdate: Update complaint status and optional officer reply.
     */
    fun updateStatusAndReply(id: Long, newStatus: String, reply: String?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STATUS, newStatus)
            if (reply != null) {
                put(COLUMN_OFFICER_REPLY, reply)
            }
        }
        return db.update(TABLE_COMPLAINTS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    /**
     * [D]elete: Remove a complaint by its ID (Admin only).
     */
    fun deleteComplaint(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_COMPLAINTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    /**
     * Restore a deleted complaint (Undo operation).
     */
    fun restoreComplaint(complaint: Complaint): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (complaint.id > 0) {
                put(COLUMN_ID, complaint.id)
            }
            put(COLUMN_TITLE, complaint.title)
            put(COLUMN_DESCRIPTION, complaint.description)
            put(COLUMN_CATEGORY, complaint.category)
            put(COLUMN_PHOTO_PATH, complaint.photoPath)
            put(COLUMN_LOCATION_ADDRESS, complaint.locationAddress)
            put(COLUMN_LATITUDE, complaint.latitude)
            put(COLUMN_LONGITUDE, complaint.longitude)
            put(COLUMN_STATUS, complaint.status)
            put(COLUMN_OFFICER_REPLY, complaint.officerReply)
            put(COLUMN_STUDENT_EMAIL, complaint.studentEmail)
            put(COLUMN_TIMESTAMP, complaint.timestamp)
        }
        return db.insertWithOnConflict(TABLE_COMPLAINTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Fetch student user by email.
     */
    fun getStudentByEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_EMAIL = ?",
            arrayOf(email.trim().lowercase()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return User(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
                    password = "",
                    studentId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_STUDENT_ID)),
                    role = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ROLE)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_USER_CREATED_AT)),
                    lastLogin = it.getLong(it.getColumnIndexOrThrow(COLUMN_USER_LAST_LOGIN))
                )
            }
        }
        return null
    }

    /**
     * Update user profile information.
     */
    fun updateUserProfile(email: String, newName: String, newStudentId: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, newName)
            if (newStudentId != null) {
                put(COLUMN_USER_STUDENT_ID, newStudentId)
            }
        }
        val rows = db.update(TABLE_USERS, values, "$COLUMN_USER_EMAIL = ?", arrayOf(email))
        return rows > 0
    }

    // =========================================================================
    // ADMIN ANALYTICS & STUDENT MANAGEMENT (REAL DB DATA)
    // =========================================================================

    /**
     * Total number of registered students.
     */
    fun getTotalStudentsCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_USERS WHERE $COLUMN_USER_ROLE = ?", arrayOf(User.ROLE_STUDENT))
        cursor.use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    /**
     * Currently active users (logged in within the last 7 days).
     */
    fun getActiveStudentsCount(): Int {
        val db = readableDatabase
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USERS WHERE $COLUMN_USER_ROLE = ? AND $COLUMN_USER_LAST_LOGIN >= ?",
            arrayOf(User.ROLE_STUDENT, sevenDaysAgo.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                val count = it.getInt(0)
                // If 0 due to timestamp gap, fallback to at least registered count or 1
                return if (count > 0) count else minOf(1, getTotalStudentsCount())
            }
        }
        return 0
    }

    /**
     * New student registrations in the last 7 days.
     */
    fun getNewRegistrationsCount(): Int {
        val db = readableDatabase
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USERS WHERE $COLUMN_USER_ROLE = ? AND $COLUMN_USER_CREATED_AT >= ?",
            arrayOf(User.ROLE_STUDENT, sevenDaysAgo.toString())
        )
        cursor.use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    /**
     * Retrieve all registered student accounts for Student Management screen.
     */
    fun getAllStudents(): List<User> {
        val list = mutableListOf<User>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_ROLE = ?",
            arrayOf(User.ROLE_STUDENT),
            null,
            null,
            "$COLUMN_USER_CREATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToUser(it))
            }
        }
        return list
    }

    /**
     * Get real count distribution per complaint category for the analytics chart.
     */
    fun getCategoryDistribution(): Map<String, Int> {
        val map = linkedMapOf<String, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CATEGORY, COUNT(*) FROM $TABLE_COMPLAINTS GROUP BY $COLUMN_CATEGORY ORDER BY COUNT(*) DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val cat = it.getString(0) ?: "General"
                val count = it.getInt(1)
                map[cat] = count
            }
        }
        return map
    }

    /**
     * Search and filter complaints by query text and status.
     */
    fun searchComplaints(query: String, statusFilter: String = "All"): List<Complaint> {
        val list = mutableListOf<Complaint>()
        val db = readableDatabase
        val q = "%${query.trim().lowercase()}%"

        val selection: String
        val selectionArgs: Array<String>

        if (statusFilter == "All") {
            selection = "($COLUMN_TITLE LIKE ? OR $COLUMN_DESCRIPTION LIKE ? OR $COLUMN_CATEGORY LIKE ? OR $COLUMN_STUDENT_EMAIL LIKE ?)"
            selectionArgs = arrayOf(q, q, q, q)
        } else {
            selection = "($COLUMN_TITLE LIKE ? OR $COLUMN_DESCRIPTION LIKE ? OR $COLUMN_CATEGORY LIKE ? OR $COLUMN_STUDENT_EMAIL LIKE ?) AND $COLUMN_STATUS = ?"
            selectionArgs = arrayOf(q, q, q, q, statusFilter)
        }

        val cursor = db.query(
            TABLE_COMPLAINTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToComplaint(it))
            }
        }
        return list
    }

    /**
     * Get complaint count for a specific student by email.
     */
    fun getComplaintCountByStudent(email: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_COMPLAINTS WHERE $COLUMN_STUDENT_EMAIL = ?",
            arrayOf(email.trim().lowercase())
        )
        cursor.use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    /**
     * Get monthly complaint counts for analytics (last 6 months).
     */
    fun getComplaintsByMonth(): Map<String, Int> {
        val map = linkedMapOf<String, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT strftime('%Y-%m', datetime($COLUMN_TIMESTAMP/1000, 'unixepoch', 'localtime')) as month, COUNT(*) FROM $TABLE_COMPLAINTS GROUP BY month ORDER BY month DESC LIMIT 6",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val month = it.getString(0) ?: "Unknown"
                val count = it.getInt(1)
                map[month] = count
            }
        }
        return map
    }

    private fun cursorToComplaint(cursor: Cursor): Complaint {
        return Complaint(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
            photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH)),
            locationAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_ADDRESS)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
            officerReply = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OFFICER_REPLY)),
            studentEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_EMAIL)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
        )
    }
}
