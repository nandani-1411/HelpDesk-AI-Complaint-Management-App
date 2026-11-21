package com.example.helpdeskai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.User
import com.example.helpdeskai.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize SQLite Database Helper & Session Manager
        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        // 2. Display Animated Splash Screen
        setContentView(R.layout.activity_splash)
        applyEdgeToEdgeInsets(findViewById(R.id.splash_root))

        val branding = findViewById<View>(R.id.layout_branding)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        branding?.startAnimation(fadeIn)

        // 3. Transition after 1.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (sessionManager.isLoggedIn()) {
                val role = sessionManager.getUserRole()
                if (role.equals(User.ROLE_ADMIN, ignoreCase = true)) {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, ComplaintListActivity::class.java))
                }
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 1500)
    }

    private fun applyEdgeToEdgeInsets(view: View?) {
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}
