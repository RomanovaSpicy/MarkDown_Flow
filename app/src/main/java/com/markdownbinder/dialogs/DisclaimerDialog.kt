package com.markdownbinder.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.markdownbinder.R
import com.markdownbinder.databinding.DialogDisclaimerBinding

interface BlurryDialogListener {
    fun onDialogShown()
    fun onDialogDismissed()
}

class DisclaimerDialog : DialogFragment() {

    private var _binding: DialogDisclaimerBinding? = null
    private val binding get() = _binding!!

    private var onComplete: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDisclaimerBinding.inflate(layoutInflater)

        setupPermissionCards()
        setupCheckboxAndNextButton()

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Widget_App_Dialog)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    override fun onStart() {
        super.onStart()
        (activity as? BlurryDialogListener)?.onDialogShown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        (activity as? BlurryDialogListener)?.onDialogDismissed()
        super.onDismiss(dialog)
    }

    private fun setupPermissionCards() {
        binding.permissionOverlayCard.setOnClickListener { requestOverlayPermission() }
        binding.overlayInfo.setOnClickListener {
            showInfoTooltip(getString(R.string.permission_overlay_description))
        }
        binding.permissionAccessibilityCard.setOnClickListener { requestAccessibilityPermission() }
        binding.accessibilityInfo.setOnClickListener {
            showInfoTooltip(getString(R.string.permission_accessibility_description))
        }
    }

    private fun setupCheckboxAndNextButton() {
        binding.nextButton.isEnabled = false

        binding.agreeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.nextButton.isEnabled = isChecked
        }

        binding.nextButton.setOnClickListener {
            onComplete?.invoke()
            dismiss()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${requireContext().packageName}")
        )
        startActivity(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun showInfoTooltip(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "DisclaimerDialog"

        fun show(
            fragmentManager: FragmentManager,
            onComplete: () -> Unit
        ) {
            val dialog = DisclaimerDialog()
            dialog.onComplete = onComplete
            dialog.show(fragmentManager, TAG)
        }
    }
}