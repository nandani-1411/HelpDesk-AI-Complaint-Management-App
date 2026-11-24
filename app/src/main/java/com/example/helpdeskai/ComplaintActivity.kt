package com.example.helpdeskai

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.CameraHelper
import com.example.helpdeskai.utils.GroqAiService
import com.example.helpdeskai.utils.LocationHelper
import com.example.helpdeskai.utils.SessionManager
import com.google.android.material.button.MaterialButton

class ComplaintActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper
    private lateinit var sessionManager: SessionManager

    // Wizard Views
    private lateinit var pbProgress: ProgressBar
    private lateinit var tvStep1Label: TextView
    private lateinit var tvStep2Label: TextView
    private lateinit var tvStep3Label: TextView
    private lateinit var layoutStep1: LinearLayout
    private lateinit var layoutStep2: LinearLayout
    private lateinit var layoutStep3: LinearLayout
    private lateinit var layoutSuccessScreen: LinearLayout
    private lateinit var layoutWizardNav: LinearLayout
    private lateinit var btnWizardBack: MaterialButton
    private lateinit var btnWizardNext: MaterialButton

    // Step 1 Views
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var autoCategory: AutoCompleteTextView
    private lateinit var btnGroqEnhance: MaterialButton
    private lateinit var layoutGroqLoading: LinearLayout

    // Step 2 Views
    private lateinit var btnCapturePhoto: MaterialButton
    private lateinit var layoutPhotoPreview: FrameLayout
    private lateinit var ivCapturedPreview: ImageView
    private lateinit var btnRemovePhoto: TextView
    private lateinit var btnFetchLocation: MaterialButton
    private lateinit var layoutLocationDetails: LinearLayout
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvLocationCoordinates: TextView

    // Step 3 Views
    private lateinit var tvReviewCategory: TextView
    private lateinit var tvReviewTitle: TextView
    private lateinit var tvReviewDesc: TextView
    private lateinit var tvReviewLocation: TextView
    private lateinit var tvReviewPhoto: TextView
    private lateinit var tvSuccessComplaintNo: TextView
    private lateinit var btnBackHomeSuccess: MaterialButton

    private var currentStep = 1
    private var capturedPhotoPath: String? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentLocationAddress: String? = null

    // Permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Camera permission needed to take photos", Toast.LENGTH_SHORT).show()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) fetchGpsLocation()
        else Toast.makeText(this, "Location permission needed to tag location", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val path = CameraHelper.saveBitmapToInternalStorage(this, bitmap)
            capturedPhotoPath = path
            ivCapturedPreview.setImageBitmap(bitmap)
            layoutPhotoPreview.visibility = View.VISIBLE
            btnCapturePhoto.text = "📷 Retake Photo"
            Toast.makeText(this, "Photo added!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_form)

        dbHelper = ComplaintDatabaseHelper(this)
        sessionManager = SessionManager(this)

        initViews()
        setupCategoryDropdown()
        setupListeners()
        updateWizardStep(1)
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_form)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            if (currentStep > 1 && currentStep <= 3) {
                updateWizardStep(currentStep - 1)
            } else {
                finish()
            }
        }

        pbProgress = findViewById(R.id.pb_wizard_progress)
        tvStep1Label = findViewById(R.id.tv_step1_label)
        tvStep2Label = findViewById(R.id.tv_step2_label)
        tvStep3Label = findViewById(R.id.tv_step3_label)

        layoutStep1 = findViewById(R.id.layout_step1)
        layoutStep2 = findViewById(R.id.layout_step2)
        layoutStep3 = findViewById(R.id.layout_step3)
        layoutSuccessScreen = findViewById(R.id.layout_success_screen)
        layoutWizardNav = findViewById(R.id.layout_wizard_nav)
        btnWizardBack = findViewById(R.id.btn_wizard_back)
        btnWizardNext = findViewById(R.id.btn_wizard_next)

        etTitle = findViewById(R.id.et_complaint_title)
        etDescription = findViewById(R.id.et_complaint_desc)
        autoCategory = findViewById(R.id.spinner_category_dropdown)
        btnGroqEnhance = findViewById(R.id.btn_groq_enhance)
        layoutGroqLoading = findViewById(R.id.layout_groq_loading)

        btnCapturePhoto = findViewById(R.id.btn_capture_photo)
        layoutPhotoPreview = findViewById(R.id.layout_photo_preview)
        ivCapturedPreview = findViewById(R.id.iv_captured_preview)
        btnRemovePhoto = findViewById(R.id.btn_remove_photo)

        btnFetchLocation = findViewById(R.id.btn_fetch_location)
        layoutLocationDetails = findViewById(R.id.layout_location_details)
        tvLocationAddress = findViewById(R.id.tv_location_address)
        tvLocationCoordinates = findViewById(R.id.tv_location_coordinates)

        tvReviewCategory = findViewById(R.id.tv_review_category)
        tvReviewTitle = findViewById(R.id.tv_review_title)
        tvReviewDesc = findViewById(R.id.tv_review_desc)
        tvReviewLocation = findViewById(R.id.tv_review_location)
        tvReviewPhoto = findViewById(R.id.tv_review_photo)

        tvSuccessComplaintNo = findViewById(R.id.tv_success_complaint_no)
        btnBackHomeSuccess = findViewById(R.id.btn_back_home_success)
    }

    private fun setupCategoryDropdown() {
        val rawCategories = resources.getStringArray(R.array.complaint_categories)
        val validCategories = rawCategories.filter { !it.startsWith("Select") }
        val categoryAdapter = ArrayAdapter(this, R.layout.item_spinner_text, validCategories)
        autoCategory.setAdapter(categoryAdapter)
        autoCategory.setText(validCategories.firstOrNull() ?: "Hostel & Mess", false)
    }

    private fun setupListeners() {
        btnGroqEnhance.setOnClickListener {
            var title = etTitle.text.toString().trim()
            val desc = etDescription.text.toString().trim()
            if (desc.isEmpty() && title.isEmpty()) {
                Toast.makeText(this, "Please enter a title or short description first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Spell check title before AI polish
            if (title.isNotEmpty()) {
                val corrected = spellCheckTitle(title)
                if (corrected != title) {
                    title = corrected
                    etTitle.setText(corrected)
                }
            }

            layoutGroqLoading.visibility = View.VISIBLE
            btnGroqEnhance.isEnabled = false

            GroqAiService.processStudentComplaint(
                title = if (title.isNotEmpty()) title else "Complaint",
                rawDescription = if (desc.isNotEmpty()) desc else title,
                onSuccess = { result ->
                    layoutGroqLoading.visibility = View.GONE
                    btnGroqEnhance.isEnabled = true
                    etDescription.setText(result.polishedDescription)
                    if (result.category.isNotEmpty()) {
                        autoCategory.setText(result.category, false)
                    }
                    Toast.makeText(this, "Description improved by AI!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    layoutGroqLoading.visibility = View.GONE
                    btnGroqEnhance.isEnabled = true
                    Toast.makeText(this, "AI note: $err", Toast.LENGTH_SHORT).show()
                }
            )
        }

        btnCapturePhoto.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnRemovePhoto.setOnClickListener {
            capturedPhotoPath = null
            layoutPhotoPreview.visibility = View.GONE
            btnCapturePhoto.text = "📷 Take Photo"
        }

        btnFetchLocation.setOnClickListener {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        btnWizardNext.setOnClickListener {
            when (currentStep) {
                1 -> {
                    val title = etTitle.text.toString().trim()
                    val desc = etDescription.text.toString().trim()
                    if (title.isEmpty()) {
                        etTitle.error = "Please enter a problem title"
                        etTitle.requestFocus()
                        return@setOnClickListener
                    }
                    if (desc.isEmpty()) {
                        etDescription.error = "Please describe the problem"
                        etDescription.requestFocus()
                        return@setOnClickListener
                    }
                    updateWizardStep(2)
                }
                2 -> {
                    updateWizardStep(3)
                }
                3 -> {
                    sendComplaint()
                }
            }
        }

        btnWizardBack.setOnClickListener {
            if (currentStep > 1) {
                updateWizardStep(currentStep - 1)
            }
        }

        btnBackHomeSuccess.setOnClickListener {
            finish()
        }
    }

    private fun updateWizardStep(step: Int) {
        currentStep = step
        pbProgress.progress = step

        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val mutedColor = ContextCompat.getColor(this, R.color.text_muted)

        tvStep1Label.setTextColor(if (step >= 1) activeColor else mutedColor)
        tvStep2Label.setTextColor(if (step >= 2) activeColor else mutedColor)
        tvStep3Label.setTextColor(if (step >= 3) activeColor else mutedColor)

        layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE
        layoutSuccessScreen.visibility = View.GONE

        btnWizardBack.visibility = if (step > 1) View.VISIBLE else View.GONE
        btnWizardNext.text = if (step == 3) "Send Complaint ✓" else "Next Step →"

        if (step == 3) {
            tvReviewCategory.text = autoCategory.text.toString()
            tvReviewTitle.text = etTitle.text.toString().trim()
            tvReviewDesc.text = etDescription.text.toString().trim()
            tvReviewLocation.text = if (!currentLocationAddress.isNullOrEmpty()) "📍 Location: $currentLocationAddress" else "📍 Location: Not tagged"
            tvReviewPhoto.text = if (capturedPhotoPath != null) "📷 Photo: Attached" else "📷 Photo: None"
        }
    }

    private fun openCamera() {
        takePictureLauncher.launch(null)
    }

    private fun fetchGpsLocation() {
        btnFetchLocation.isEnabled = false
        btnFetchLocation.text = "⏳ Getting Location..."

        LocationHelper.fetchCurrentLocation(
            context = this,
            onLocationReceived = { lat, lng, address ->
                currentLatitude = lat
                currentLongitude = lng
                currentLocationAddress = address
                btnFetchLocation.isEnabled = true
                btnFetchLocation.text = "📍 Update Location"
                tvLocationAddress.text = "Location: $address"
                tvLocationCoordinates.text = "GPS: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
                layoutLocationDetails.visibility = View.VISIBLE
                Toast.makeText(this, "Location found!", Toast.LENGTH_SHORT).show()
            },
            onError = { errMsg ->
                btnFetchLocation.isEnabled = true
                btnFetchLocation.text = "📍 Get Current Location"
                Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun sendComplaint() {
        btnWizardNext.isEnabled = false
        btnWizardNext.text = "Sending..."

        val title = etTitle.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        val cat = autoCategory.text.toString().trim()
        val studentEmail = sessionManager.getUserEmail()

        val complaint = Complaint(
            title = title,
            description = desc,
            category = cat,
            photoPath = capturedPhotoPath,
            locationAddress = currentLocationAddress,
            latitude = currentLatitude,
            longitude = currentLongitude,
            status = "Pending",
            officerReply = null,
            studentEmail = studentEmail,
            timestamp = System.currentTimeMillis()
        )

        val rowId = dbHelper.insertComplaint(complaint)
        if (rowId != -1L) {
            layoutStep1.visibility = View.GONE
            layoutStep2.visibility = View.GONE
            layoutStep3.visibility = View.GONE
            layoutWizardNav.visibility = View.GONE
            pbProgress.visibility = View.GONE
            findViewById<View>(R.id.tv_step1_label).parent.let { (it as? View)?.visibility = View.GONE }

            tvSuccessComplaintNo.text = "Complaint Sent Successfully"
            layoutSuccessScreen.visibility = View.VISIBLE
        } else {
            btnWizardNext.isEnabled = true
            btnWizardNext.text = "Send Complaint ✓"
            Toast.makeText(this, "Failed to send complaint. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun spellCheckTitle(input: String): String {
        val commonCorrections = mapOf(
            "hosetl" to "Hostel", "hostle" to "Hostel",
            "washrom" to "Washroom", "washrooom" to "Washroom", "bathrom" to "Bathroom",
            "facilty" to "Facility", "faclity" to "Facility",
            "leekage" to "Leakage", "leeking" to "Leaking", "leakge" to "Leakage",
            "complnt" to "Complaint", "complaint" to "Complaint", "grivance" to "Complaint",
            "electrcity" to "Electricity", "electicity" to "Electricity",
            "intenet" to "Internet", "wifi" to "Wi-Fi", "wifii" to "Wi-Fi",
            "profesor" to "Professor", "librry" to "Library", "libary" to "Library",
            "clen" to "Clean", "clening" to "Cleaning",
            "watr" to "Water", "watter" to "Water",
            "lightt" to "Light", "fann" to "Fan",
            "problm" to "Problem", "isue" to "Issue",
            "canteen" to "Canteen", "cantin" to "Canteen"
        )
        return input.split("\\s+".toRegex()).joinToString(" ") { word ->
            val clean = word.lowercase().filter { it.isLetter() }
            val rep = commonCorrections[clean]
            if (rep != null) {
                if (word.isNotEmpty() && word[0].isUpperCase()) rep.replaceFirstChar { it.uppercase() } else rep.lowercase()
            } else {
                word
            }
        }
    }
}
