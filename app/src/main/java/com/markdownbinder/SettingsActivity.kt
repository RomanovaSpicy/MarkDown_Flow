package com.markdownbinder

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.markdownbinder.adapters.Language
import com.markdownbinder.adapters.LanguageAdapter
import com.markdownbinder.database.BindRepository
import com.markdownbinder.databinding.ActivitySettingsBinding
import com.markdownbinder.models.Bind
import com.markdownbinder.utils.PreferenceManager
import com.markdownbinder.utils.StatisticsManager
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SettingsActivity - Settings screen
 *
 * Functionality:
 * - Live Preview of the overlay (changes when the sliders move)
 * - Width slider
 * - Height slider
 * - Language selection
 * - About section (version, build, developer)
 * - Data management (export, import, clear)
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: BindRepository
    private lateinit var statsManager: StatisticsManager
    private lateinit var languageAdapter: LanguageAdapter

    private var overlayWidth = 160 // default
    private var overlayHeight = 5 // default rows
    private var selectedLanguage = "en"

    // List of languages
    private val languages = listOf(
        Language("en", "🇬🇧", R.string.lang_en),
        Language("ru", "🇷🇺", R.string.lang_ru),
        Language("fr", "🇫🇷", R.string.lang_fr),
        Language("de", "🇩🇪", R.string.lang_de),
        Language("it", "🇮🇹", R.string.lang_it),
        Language("ja", "🇯🇵", R.string.lang_ja),
        Language("zh", "🇨🇳", R.string.lang_zh),
        Language("pl", "🇵🇱", R.string.lang_pl),
        Language("hi", "🇮🇳", R.string.lang_hi),
        Language("es", "🇪🇸", R.string.lang_es),
        Language("pt","🇵🇹", R.string.lang_pt)
    )

    companion object {
        private const val REQUEST_CODE_EXPORT = 101
        private const val REQUEST_CODE_IMPORT = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BindRepository(this)
        statsManager = StatisticsManager(this)

        loadSettings()
        setupSliders()
        setupLanguageGrid()
        setupButtons()
        setupAboutCard()
        updatePreview()
    }

    /**
     * Loading saved settings
     */
    private fun loadSettings() {
        overlayWidth = PreferenceManager.getOverlayWidth(this)
        overlayHeight = PreferenceManager.getOverlayHeight(this)
        selectedLanguage = PreferenceManager.getLanguage(this)
    }

    /**
     * Configuring sliders from live preview
     */
    private fun setupSliders() {
        // Width slider
        binding.widthSlider.value = overlayWidth.toFloat()
        binding.widthValue.text = "$overlayWidth dp"

        binding.widthSlider.addOnChangeListener { _, value, _ ->
            overlayWidth = value.toInt()
            binding.widthValue.text = "$overlayWidth dp"
            updatePreview()
            PreferenceManager.setOverlayWidth(this, overlayWidth)
        }

        // Height slider
        binding.heightSlider.value = overlayHeight.toFloat()
        binding.heightValue.text = "$overlayHeight rows"

        binding.heightSlider.addOnChangeListener { _, value, _ ->
            overlayHeight = value.toInt()
            binding.heightValue.text = "$overlayHeight rows"
            updatePreview()
            PreferenceManager.setOverlayHeight(this, overlayHeight)
        }
    }

    /**
     * Updating the live preview overlay
     */
    private fun updatePreview() {
        val previewCard = binding.overlayPreview.overlayPreviewCard

        val params = previewCard.layoutParams
        params.width = dpToPx(overlayWidth)
        previewCard.layoutParams = params

        val container = binding.overlayPreview.previewItemsContainer
        container.removeAllViews()

        for (i in 1..overlayHeight) {
            val itemView = LayoutInflater.from(this).inflate(
                R.layout.item_preview_row,
                container,
                false
            )

            val textView = itemView.findViewById<TextView>(R.id.preview_row_text)
            textView.text = getString(R.string.overlay_preview_row, i)

            container.addView(itemView)
        }
    }

    /**
     * Setting up the language grid (2 columns)
     */
    private fun setupLanguageGrid() {
        languageAdapter = LanguageAdapter { language ->
            onLanguageSelected(language)
        }

        binding.languagesRecycler.apply {
            layoutManager = GridLayoutManager(this@SettingsActivity, 2)
            adapter = languageAdapter
        }

        languageAdapter.submitList(languages)
        languageAdapter.setSelectedLanguage(selectedLanguage)
    }

    /**
     * Language selection processing
     */
    private fun onLanguageSelected(language: Language) {
        // Haptic feedback
        binding.root.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        selectedLanguage = language.code
        PreferenceManager.setLanguage(this, selectedLanguage)
        languageAdapter.setSelectedLanguage(selectedLanguage)

        MaterialAlertDialogBuilder(this)
            .setTitle("Language Changed")
            .setMessage("Please restart the app to apply language changes.")
            .setPositiveButton(R.string.ok) { _, _ ->
                // Restart app
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Setting up Buttons
     */
    private fun setupButtons() {
        // Back button
        binding.backButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            finish()
        }

        // Export binds
        binding.exportButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            exportBinds()
        }

        // Import binds
        binding.importButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            importBinds()
        }

        // Clear all data
        binding.clearButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            confirmClearAllData()
        }
    }

    private fun setupAboutCard() {
        val inspiredByTextView = binding.inspiredByText
        val fullText = "Inspired by SpicyChat.AI"
        val linkText = "SpicyChat.AI"
        val url = "https://spicychat.ai"

        val spannableString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // Remove underline
                ds.color = ContextCompat.getColor(this@SettingsActivity, R.color.purple_200) // Set link color
            }
        }

        val start = fullText.indexOf(linkText)
        val end = start + linkText.length

        if (start != -1) {
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        inspiredByTextView.text = spannableString
        inspiredByTextView.movementMethod = LinkMovementMethod.getInstance()

        binding.githubLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/RomanovaSpicy/MarkDown_Flow".toUri())
            startActivity(intent)
        }
    }

    /**
     * Exporting binds to JSON
     */
    private fun exportBinds() {
        lifecycleScope.launch {
            try {
                repository.getAllBinds().collect { binds ->
                    if (binds.isEmpty()) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "No binds to export",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@collect
                    }

                    val gson = Gson()
                    val json = gson.toJson(binds)

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "markdown_binder_binds.json")
                    }

                    startActivityForResult(intent, REQUEST_CODE_EXPORT)

                    PreferenceManager.setTempExportData(this@SettingsActivity, json)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Importing binds from JSON
     */
    private fun importBinds() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }

        startActivityForResult(intent, REQUEST_CODE_IMPORT)
    }

    /**
     * Confirmation of clearing all data
     */
    private fun confirmClearAllData() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear All Data")
            .setMessage("This will delete all binds and reset statistics. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Clearing all data
     */
    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                repository.deleteAllBinds()

                statsManager.clearStatistics()

                val isFirstLaunch = PreferenceManager.isFirstLaunch(this@SettingsActivity)
                PreferenceManager.clearAll(this@SettingsActivity)
                if (!isFirstLaunch) {
                    PreferenceManager.setFirstLaunchComplete(this@SettingsActivity)
                }

                Toast.makeText(
                    this@SettingsActivity,
                    "All data cleared",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Failed to clear data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_EXPORT -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { uri ->
                        val json = PreferenceManager.getTempExportData(this)
                        try {
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(json.toByteArray())
                            }

                            Toast.makeText(this, "Binds exported successfully", Toast.LENGTH_SHORT).show()
                            PreferenceManager.clearTempExportData(this)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            REQUEST_CODE_IMPORT -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { uri ->
                        try {
                            val json = contentResolver.openInputStream(uri)?.use { inputStream ->
                                BufferedReader(InputStreamReader(inputStream)).readText()
                            }

                            if (json != null) {
                                val gson = Gson()
                                val type = object : TypeToken<List<Bind>>() {}.type
                                val binds: List<Bind> = gson.fromJson(json, type)

                                lifecycleScope.launch {
                                    repository.insertBinds(binds)
                                    Toast.makeText(
                                        this@SettingsActivity,
                                        "Imported ${binds.size} binds",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Converting dp to px
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
