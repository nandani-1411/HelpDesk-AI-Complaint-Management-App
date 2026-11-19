package com.example.helpdeskai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.LocationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize SQLite Database Helper
        dbHelper = ComplaintDatabaseHelper(this)

        // 2. Start by displaying Animated Splash Screen
        setContentView(R.layout.activity_splash)
        applyEdgeToEdgeInsets(findViewById(R.id.splash_root))

        val logo = findViewById<View>(R.id.iv_logo)
        val branding = findViewById<View>(R.id.layout_branding)
        
        // Start Fade-in Animation on logo & title
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        branding.startAnimation(fadeIn)

        // 3. Transition to Role Selection after 1.8 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            showRoleSelectionScreen()
        }, 1800)
    }

    private fun showRoleSelectionScreen() {
        setContentView(R.layout.activity_role_select)
        applyEdgeToEdgeInsets(findViewById(R.id.role_select_root))

        val root = findViewById<View>(R.id.role_select_root)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        root.startAnimation(slideUp)

        // Student Card Click Listener
        findViewById<View>(R.id.card_student_role)?.setOnClickListener {
            Toast.makeText(this, "Opening Student Complaint Portal...", Toast.LENGTH_SHORT).show()
        }

        // Admin Card Click Listener
        findViewById<View>(R.id.card_admin_role)?.setOnClickListener {
            val count = dbHelper.getAllComplaints().size
            Toast.makeText(this, "Opening Officer Portal ($count complaints in DB)", Toast.LENGTH_SHORT).show()
        }
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
