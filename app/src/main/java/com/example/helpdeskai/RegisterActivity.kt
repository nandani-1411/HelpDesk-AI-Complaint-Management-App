package com.example.helpdeskai

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var etName: EditText
    private lateinit var etStudentId: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var tvGoToLogin: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.et_reg_name)
        etStudentId = findViewById(R.id.et_reg_student_id)
        etEmail = findViewById(R.id.et_reg_email)
        etPassword = findViewById(R.id.et_reg_password)
        etConfirmPassword = findViewById(R.id.et_reg_confirm_password)
        ivTogglePassword = findViewById(R.id.iv_reg_toggle_password)
        ivToggleConfirmPassword = findViewById(R.id.iv_reg_toggle_confirm_password)
        btnSubmit = findViewById(R.id.btn_reg_submit)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)
    }

    private fun setupListeners() {
        ivTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_on)
            }
            isPasswordVisible = !isPasswordVisible
            etPassword.setSelection(etPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_on)
            }
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        btnSubmit.setOnClickListener {
            performRegistration()
        }

        findViewById<ImageView>(R.id.iv_reg_back)?.setOnClickListener {
            finish()
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegistration() {
        val name = etName.text.toString().trim()
        val studentId = etStudentId.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Please enter your full name"
            etName.requestFocus()
            return
        }

        if (studentId.isEmpty()) {
            etStudentId.error = "Please enter your student ID"
            etStudentId.requestFocus()
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email address"
            etEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        val rowId = dbHelper.registerStudent(name, email, password, studentId)
        if (rowId > 0) {
            // Auto-login newly registered student directly to their dashboard
            val user = com.example.helpdeskai.data.model.User(
                id = rowId,
                name = name,
                email = email.lowercase(),
                password = password,
                studentId = studentId,
                role = com.example.helpdeskai.data.model.User.ROLE_STUDENT,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            sessionManager.saveUserSession(user)
            Toast.makeText(this, "🎉 Welcome, $name! Your account has been created.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, ComplaintListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Registration failed. This email is already registered.", Toast.LENGTH_LONG).show()
        }
    }
}
