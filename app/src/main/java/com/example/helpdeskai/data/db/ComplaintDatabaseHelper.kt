package com.example.helpdeskai.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.helpdeskai.data.model.Complaint

/**
 * SQLite Database Helper managing local persistent storage for Complaints (CRUD).
 */
class ComplaintDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "helpdesk_ai.db"
        private const val DATABASE_VERSION = 1

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
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
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
                $COLUMN_TIMESTAMP INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMPLAINTS")
        onCreate(db)
    }

    /**
     * [C]reate: Insert a new complaint into the database.
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
            put(COLUMN_TIMESTAMP, complaint.timestamp)
        }
        return db.insert(TABLE_COMPLAINTS, null, values)
    }

    /**
     * [R]ead: Get all complaints ordered by newest first.
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
     * [R]ead: Filter complaints by status (Pending, In Progress, Resolved).
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
     * [D]elete: Remove a complaint by its ID.
     */
    fun deleteComplaint(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_COMPLAINTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
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
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
        )
    }
}
