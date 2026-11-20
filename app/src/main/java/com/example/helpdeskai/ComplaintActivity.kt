package com.example.helpdeskai

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.helpdeskai.data.db.ComplaintDatabaseHelper
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.CameraHelper
import com.example.helpdeskai.utils.GroqAiService
import com.example.helpdeskai.utils.LocationHelper
import com.google.android.material.button.MaterialButton

class ComplaintActivity : AppCompatActivity() {

    private lateinit var dbHelper: ComplaintDatabaseHelper

    // UI elements
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnGroqEnhance: MaterialButton
    private lateinit var layoutGroqLoading: LinearLayout
    private lateinit var spinnerCategory: Spinner
    private lateinit var layoutAiSuggestion: LinearLayout
    private lateinit var tvAiBadgeHeader: TextView
    private lateinit var tvAiPredictionText: TextView
    private lateinit var btnApplyAiSuggestion: TextView

    private lateinit var btnCapturePhoto: MaterialButton
    private lateinit var layoutPhotoPreview: FrameLayout
    private lateinit var ivCapturedPreview: ImageView
    private lateinit var btnRemovePhoto: TextView

    private lateinit var btnFetchLocation: MaterialButton
    private lateinit var layoutLocationDetails: LinearLayout
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvLocationCoordinates: TextView

    private lateinit var btnSubmit: MaterialButton

    // Data state
    private var capturedPhotoPath: String? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentLocationAddress: String? = null
    private var aiPredictedCategory: String? = null

    // Camera launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = CameraHelper.saveBitmapToInternalStorage(this, bitmap)
            if (savedPath != null) {
                capturedPhotoPath = savedPath
                ivCapturedPreview.setImageBitmap(bitmap)
                layoutPhotoPreview.visibility = View.VISIBLE
                Toast.makeText(this, "Photo attached successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for Location
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchGpsCoordinates()
        } else {
            Toast.makeText(this, "Location permission is required to tag grievance location", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Camera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(this, "Camera permission required to capture evidence", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_form)

        dbHelper = ComplaintDatabaseHelper(this)

        initViews()
        setupToolbar()
        setupSpinner()
        setupAiEnhance()
        setupAiTextWatcher()
        setupListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.et_complaint_title)
        etDescription = findViewById(R.id.et_complaint_description)
        btnGroqEnhance = findViewById(R.id.btn_groq_enhance)
        layoutGroqLoading = findViewById(R.id.layout_groq_loading)
        spinnerCategory = findViewById(R.id.spinner_category)
        layoutAiSuggestion = findViewById(R.id.layout_ai_suggestion)
        tvAiBadgeHeader = findViewById(R.id.tv_ai_badge_header)
        tvAiPredictionText = findViewById(R.id.tv_ai_prediction_text)
        btnApplyAiSuggestion = findViewById(R.id.btn_apply_ai_suggestion)

        btnCapturePhoto = findViewById(R.id.btn_capture_photo)
        layoutPhotoPreview = findViewById(R.id.layout_photo_preview)
        ivCapturedPreview = findViewById(R.id.iv_captured_preview)
        btnRemovePhoto = findViewById(R.id.btn_remove_photo)

        btnFetchLocation = findViewById(R.id.btn_fetch_location)
        layoutLocationDetails = findViewById(R.id.layout_location_details)
        tvLocationAddress = findViewById(R.id.tv_location_address)
        tvLocationCoordinates = findViewById(R.id.tv_location_coordinates)

        btnSubmit = findViewById(R.id.btn_submit)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_form)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.complaint_categories,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    /**
     * Groq LLM Auto-Enhance button — sends complaint to Groq for:
     *   1. Auto-categorization (LLM decides department, not if-else)
     *   2. Structured description rewrite (LLM writes professional format)
     *   3. Urgency assessment (LLM decides based on context)
     */
    private fun setupAiEnhance() {
        btnGroqEnhance.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc  = etDescription.text.toString().trim()

            if (title.isEmpty() && desc.isEmpty()) {
                Toast.makeText(this, "Please enter a title or description first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading spinner while Groq processes
            layoutGroqLoading.visibility = View.VISIBLE
            btnGroqEnhance.isEnabled = false
            layoutAiSuggestion.visibility = View.GONE

            GroqAiService.processStudentComplaint(
                title          = title.ifEmpty { "Grievance Report" },
                rawDescription = desc.ifEmpty { title },
                onSuccess = { result ->
                    layoutGroqLoading.visibility = View.GONE
                    btnGroqEnhance.isEnabled = true

                    // LLM auto-selects the category from the spinner
                    selectSpinnerItem(result.category)
                    aiPredictedCategory = result.category

                    // LLM rewrites the description in structured professional format
                    etDescription.setText(result.polishedDescription)

                    // Show Groq verification badge
                    tvAiBadgeHeader.text    = "✅ Groq LLM — Auto-Categorized & Structured"
                    tvAiPredictionText.text = "Category: ${result.category}  •  Urgency: ${result.urgency}  •  AI Confidence: ${result.confidence}%"
                    layoutAiSuggestion.visibility = View.VISIBLE

                    Toast.makeText(this, "✨ Groq AI has structured and categorized your complaint!", Toast.LENGTH_LONG).show()
                },
                onError = { errorMessage ->
                    layoutGroqLoading.visibility = View.GONE
                    btnGroqEnhance.isEnabled = true

                    // Show error — Groq is not reachable (no internet or API issue)
                    tvAiBadgeHeader.text    = "⚠️ Groq AI Unavailable"
                    tvAiPredictionText.text = "Please check internet connection and try again."
                    layoutAiSuggestion.visibility = View.VISIBLE

                    Toast.makeText(this, "Groq AI error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun setupAiTextWatcher() {
        // Just enable the Groq button hint when user starts typing
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasContent = (etTitle.text.toString().trim().length + etDescription.text.toString().trim().length) >= 5
                btnGroqEnhance.isEnabled = hasContent
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etTitle.addTextChangedListener(textWatcher)
        etDescription.addTextChangedListener(textWatcher)

        btnApplyAiSuggestion.setOnClickListener {
            aiPredictedCategory?.let { category ->
                selectSpinnerItem(category)
                Toast.makeText(this, "Category applied: $category", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectSpinnerItem(categoryName: String) {
        val count = spinnerCategory.adapter.count
        for (i in 0 until count) {
            val item = spinnerCategory.adapter.getItem(i).toString()
            if (item.equals(categoryName, ignoreCase = true)) {
                spinnerCategory.setSelection(i)
                break
            }
        }
    }

    private fun setupListeners() {
        btnCapturePhoto.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnRemovePhoto.setOnClickListener {
            capturedPhotoPath = null
            layoutPhotoPreview.visibility = View.GONE
            ivCapturedPreview.setImageDrawable(null)
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }

        btnFetchLocation.setOnClickListener {
            if (LocationHelper.hasLocationPermission(this)) {
                fetchGpsCoordinates()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        btnSubmit.setOnClickListener {
            submitGrievance()
        }
    }

    private fun fetchGpsCoordinates() {
        btnFetchLocation.isEnabled = false
        btnFetchLocation.text = "Detecting GPS..."

        LocationHelper.fetchCurrentLocation(
            context = this,
            onLocationReceived = { lat, lng, address ->
                currentLatitude = lat
                currentLongitude = lng
                currentLocationAddress = address

                tvLocationAddress.text = address
                tvLocationCoordinates.text = "Lat: %.4f, Long: %.4f".format(lat, lng)
                layoutLocationDetails.visibility = View.VISIBLE

                btnFetchLocation.isEnabled = true
                btnFetchLocation.text = "Update GPS Location"
                Toast.makeText(this, "Location detected", Toast.LENGTH_SHORT).show()
            },
            onError = { errorMsg ->
                btnFetchLocation.isEnabled = true
                btnFetchLocation.text = "Fetch Current GPS"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun submitGrievance() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val selectedCategory = spinnerCategory.selectedItem?.toString() ?: ""

        if (title.isEmpty()) {
            etTitle.error = "Please enter complaint title"
            etTitle.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Please provide details"
            etDescription.requestFocus()
            return
        }

        if (selectedCategory == "Select Category" || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select an issue category", Toast.LENGTH_SHORT).show()
            return
        }

        val complaint = Complaint(
            title = title,
            description = description,
            category = selectedCategory,
            photoPath = capturedPhotoPath,
            locationAddress = currentLocationAddress,
            latitude = currentLatitude,
            longitude = currentLongitude,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )

        val rowId = dbHelper.insertComplaint(complaint)
        if (rowId > 0) {
            Toast.makeText(this, "Grievance #$rowId submitted successfully!", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to submit grievance. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
