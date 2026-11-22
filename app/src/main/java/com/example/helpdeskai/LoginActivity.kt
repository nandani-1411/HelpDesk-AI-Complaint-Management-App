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
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var tvGoToRegister: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        dbHelper = ComplaintDatabaseHelper(this)

        // If user already logged in, route immediately
        if (sessionManager.isLoggedIn()) {
            navigateBasedOnRole(sessionManager.getUserRole())
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.et_login_email)
        etPassword = findViewById(R.id.et_login_password)
        ivTogglePassword = findViewById(R.id.iv_toggle_password)
        btnSubmit = findViewById(R.id.btn_login_submit)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)

        // Handle prefilled email or post-registration banner if passed
        val prefilledEmail = intent.getStringExtra("EXTRA_PREFILLED_EMAIL")
        if (!prefilledEmail.isNullOrBlank()) {
            etEmail.setText(prefilledEmail)
            etPassword.requestFocus()
        }
        if (intent.getBooleanExtra("EXTRA_REG_SUCCESS", false)) {
            Toast.makeText(this, "Account created! Please sign in with your password.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        btnSubmit.setOnClickListener {
            performLogin()
        }

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun togglePasswordVisibility() {
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

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Please enter email"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Please enter password"
            etPassword.requestFocus()
            return
        }

        val user = dbHelper.login(email, password)
        if (user != null) {
            sessionManager.saveUserSession(user)
            Toast.makeText(this, "Welcome, ${user.name}!", Toast.LENGTH_SHORT).show()
            navigateBasedOnRole(user.role)
            finish()
        } else {
            Toast.makeText(this, "Invalid email or password. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateBasedOnRole(role: String) {
        val intent = if (role.equals(User.ROLE_ADMIN, ignoreCase = true)) {
            Intent(this, AdminDashboardActivity::class.java)
        } else {
            Intent(this, ComplaintListActivity::class.java)
        }
        startActivity(intent)
    }
}
