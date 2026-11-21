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
                $COLUMN_USER_ROLE TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // Seed Master Admin Account
        val adminValues = ContentValues().apply {
            put(COLUMN_USER_NAME, "College Grievance Officer")
            put(COLUMN_USER_EMAIL, "admin@gmail.com")
            put(COLUMN_USER_PASSWORD, "admin123")
            put(COLUMN_USER_STUDENT_ID, "STAFF-001")
            put(COLUMN_USER_ROLE, User.ROLE_ADMIN)
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
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, name.trim())
            put(COLUMN_USER_EMAIL, email.trim().lowercase())
            put(COLUMN_USER_PASSWORD, pass.trim())
            put(COLUMN_USER_STUDENT_ID, studentId.trim().uppercase())
            put(COLUMN_USER_ROLE, User.ROLE_STUDENT)
        }
        return try {
            db.insertOrThrow(TABLE_USERS, null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Authenticate user credentials.
     * @return User object if valid, null if invalid credentials.
     */
    fun login(email: String, pass: String): User? {
        val db = readableDatabase
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
                return User(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_EMAIL)),
                    password = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_PASSWORD)),
                    studentId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_STUDENT_ID)),
                    role = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ROLE))
                )
            }
        }
        return null
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
