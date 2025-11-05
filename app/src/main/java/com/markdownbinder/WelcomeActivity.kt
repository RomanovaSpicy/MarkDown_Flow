package com.markdownbinder

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.markdownbinder.adapters.Language
import com.markdownbinder.database.BindRepository
import com.markdownbinder.databinding.ActivityWelcomeBinding
import com.markdownbinder.dialogs.BlurryDialogListener
import com.markdownbinder.dialogs.DisclaimerDialog
import com.markdownbinder.models.Bind
import com.markdownbinder.utils.PreferenceManager
import kotlinx.coroutines.launch

class WelcomeActivity : BaseActivity(), BlurryDialogListener {

    private lateinit var binding: ActivityWelcomeBinding
    private var cursorAnimator: ValueAnimator? = null
    private lateinit var selectedLanguage: String
    private lateinit var repository: BindRepository

    // List of languages with flags (codes must correspond to the values-* folders)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PreferenceManager.isFirstLaunch(this)) {
            navigateToMain()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BindRepository(this)
        selectedLanguage = PreferenceManager.getLanguage(this, "en")

        setupLanguageChips()
        setupCursorAnimation()
        setupContinueButton()
    }

    private fun setupContinueButton() {
        binding.continueButton.setOnClickListener {
            showDisclaimerDialog()
        }
    }

    private fun showDisclaimerDialog() {
        DisclaimerDialog.show(
            fragmentManager = supportFragmentManager,
            onComplete = {
                lifecycleScope.launch {
                    createPresetBinds()
                    navigateToMain()
                }
            }
        )
    }

    // Preset configuration of bindings
    private suspend fun createPresetBinds() {
        val presets = listOf(
            Bind(name = getString(R.string.preset_dialogue), content = "\"—|\"", order = 0),
            Bind(name = getString(R.string.preset_action), content = "*|*", order = 1),
            Bind(name = getString(R.string.preset_bold), content = "**|**", order = 2),
            Bind(name = getString(R.string.preset_strikethrough), content = "~~|~~", order = 3),
            Bind(name = getString(R.string.preset_code_block), content = "***|***", order = 4),
            Bind(name = getString(R.string.preset_code), content = "`|`", order = 5),
            Bind(name = getString(R.string.preset_heading_1), content = "# |", order = 6),
            Bind(name = getString(R.string.preset_heading_2), content = "## |", order = 7),
            Bind(name = getString(R.string.preset_heading_3), content = "### |", order = 8),
            Bind(name = getString(R.string.preset_list), content = "1. |", order = 9),
            Bind(name = getString(R.string.preset_image), content = "![M](url|)", order = 10)
        )

        presets.forEach { repository.insertBind(it) }
        PreferenceManager.setFirstLaunchComplete(this)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDialogShown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR)
            binding.root.setRenderEffect(blurEffect)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDialogDismissed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.root.setRenderEffect(null)
        }
    }

    private fun setupLanguageChips() {
        binding.languageChipGroup.removeAllViews()
        languages.forEach { language ->
            val chip = Chip(this).apply {
                tag = language.code
                text = "${language.flag} ${getString(language.nameRes)}"
                isCheckable = true
            }

            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    val clickedLanguageCode = buttonView.tag as String
                    if (selectedLanguage != clickedLanguageCode) {
                        selectedLanguage = clickedLanguageCode
                        PreferenceManager.setLanguage(this@WelcomeActivity, selectedLanguage)
                        recreate()
                    }
                }
            }
            binding.languageChipGroup.addView(chip)
        }
        updateAllChipAppearances()
    }

    private fun updateAllChipAppearances() {
        binding.languageChipGroup.children.forEach { view ->
            if (view is Chip) {
                val isSelected = view.tag == selectedLanguage
                view.isChecked = isSelected
                view.setBackgroundResource(R.drawable.bg_chip_final_with_ripple)
                view.setTextColor(
                    ContextCompat.getColor(this,
                        if (isSelected) R.color.text_primary
                        else R.color.text_secondary
                    )
                )
            }
        }
    }

    private fun setupCursorAnimation() {
        binding.demoInput.isFocusable = false
        binding.demoInput.isClickable = false
        binding.demoInput.isCursorVisible = false

        val text = "**Insert and start printing |**"
        val pipePosition = text.indexOf('|')
        if (pipePosition == -1) return

        val spannable = SpannableString(text)
        val pipeColor = binding.demoInput.currentTextColor

        binding.demoInput.setText(spannable, TextView.BufferType.SPANNABLE)

        cursorAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float

                val animatedColor = Color.argb(
                    (alpha * 255).toInt(),
                    Color.red(pipeColor),
                    Color.green(pipeColor),
                    Color.blue(pipeColor)
                )

                val editable = binding.demoInput.text as Editable

                editable.getSpans(pipePosition, pipePosition + 1, ForegroundColorSpan::class.java).forEach {
                    editable.removeSpan(it)
                }

                editable.setSpan(ForegroundColorSpan(animatedColor), pipePosition, pipePosition + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cursorAnimator?.cancel()
    }

    data class Language(
        val code: String,
        val flag: String,
        val nameRes: Int
    )
}
